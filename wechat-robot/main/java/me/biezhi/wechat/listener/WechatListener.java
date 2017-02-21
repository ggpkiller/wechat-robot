package me.biezhi.wechat.listener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blade.kit.json.JSONObject;

import me.biezhi.wechat.model.Answer;
import me.biezhi.wechat.model.WechatMeta;
import me.biezhi.wechat.service.WechatService;
import me.biezhi.wechat.service.WechatServiceImpl;

public class WechatListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(WechatListener.class);
	public static String questionFilePath = null;
	int playWeChat = 0;
	
	public void start(final WechatService wechatService, final WechatMeta wechatMeta){
		new Thread(new Runnable() {
			@Override
			public void run() {
				LOGGER.info("进入消息监听模式 ...");
				wechatService.choiceSyncLine(wechatMeta);
				while(true){
					int[] arr = wechatService.syncCheck(wechatMeta);
					LOGGER.info("retcode={}, selector={}", arr[0], arr[1]);
					
					if(arr[0] == 1100){
						LOGGER.info("你在手机上登出了微信，债见");
						break;
					}
					if(arr[0] == 0){
						if(arr[1] == 2){
							JSONObject data = wechatService.webwxsync(wechatMeta);
							wechatService.handleMsg(wechatMeta, data);
						} else if(arr[1] == 6){
							JSONObject data = wechatService.webwxsync(wechatMeta);
							wechatService.handleMsg(wechatMeta, data);
						} else if(arr[1] == 7){
							playWeChat += 1;
							LOGGER.info("你在手机上玩微信被我发现了 {} 次", playWeChat);
							wechatService.webwxsync(wechatMeta);
						} else if(arr[1] == 3){
							continue;
						} else if(arr[1] == 0){
							continue;
						}
					} else {
						// 
					}
					try {
						LOGGER.info("等待2000ms...");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}, "wechat-listener-thread").start();
		
		
		
		
		
		
		//8点开题线程
		Thread answerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				String noticeTime = "19:50";
				String now = null;
				while( !(now = sdf.format(new Date())).equals(noticeTime) ){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				String noticeMessage = "大家好[愉快]我是平安小管家，今日晚八点的车管练习题由我来发放给大家。\n现在距离开始还有10分钟，请大家做好准备，加油![愉快]";
				wechatService.webwxsendmsg(wechatMeta, noticeMessage,WechatServiceImpl.groupId);
				
				
				//读题
				InputStream ins = null;
				try {
					ins = new FileInputStream(questionFilePath);
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(ins,Charset.forName("UTF-8")));
				String line = null;
				int index = 0;
				try {
					WechatServiceImpl.questions.clear();
					Answer answer = null;
					String[] options = null;
					int choiceQuestionState = 0;
					while(!StringUtils.isBlank( line = reader.readLine() )){
						line = line.trim();
						if( line.charAt(0) == '#' || choiceQuestionState ==1 ){
							//选择题
							choiceQuestionState = 1;
							if(index == 0){ //问题
								//初始化问题
								answer = new Answer();
								answer.setQuestionType(1);
								options = new String[4];
								line = "选择题：\n" + line.substring(1);
								answer.setQuestion(line);
								index++;
							}else if(index >0 && index < 5){
								//选择项
								options[index - 1] = line;
								index++;
							}else if(index == 5){
								answer.setAnswer(line);
								answer.setOptions(options);
								index = 0;
								choiceQuestionState = 0;
								WechatServiceImpl.questions.add(answer);
							}
						}else if(line.charAt(0) == '$' || choiceQuestionState == -1){
							//判断题
							choiceQuestionState = -1;
							if(index == 0){
								//题
								answer = new Answer();
								answer.setQuestionType(-1);
								line = "判断题：\n" + line.substring(1);
								index++;
							}else if(index == 1){
								//答案
								answer.setAnswer(line);
								index = 0;
								choiceQuestionState = 0;
								WechatServiceImpl.questions.add(answer);
							}
						}
					}
					reader.close();
					for(Answer a:WechatServiceImpl.questions){
						System.out.println(a.getQuestion());
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				//答题开始
				String startTime = "20:00";
				while( !(now = sdf.format(new Date())).equals(startTime) ){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				String startMessage = "我是平安小管家[愉快]，没错又是我[鼓掌]，今天的练习题正式开始。\n题目分为选择题和判断题：\n选择题有\"ABCD\"四个选项，选择其中一个正确答案即可（不分大小写）；\n判断题直接回复答案\"对/错\"即可。\n每题之间间隔20秒，请在20秒内作答。\nPS:回复答案时不用@本管家，直接输入您的正确答案即可。";
				wechatService.webwxsendmsg(wechatMeta, startMessage,WechatServiceImpl.groupId);
				
				//间隔20秒
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				
				
				//正式发放题
				int total = WechatServiceImpl.questions.size();
				for (int i = 0; i < total; i++) {
					WechatServiceImpl.index=i;
					Answer answer = WechatServiceImpl.questions.get(i);
					if(answer.getQuestionType() == 1){
						//选择题
						StringBuilder questionContent = new StringBuilder();
						String question = answer.getQuestion();
						WechatServiceImpl.question = question;
						WechatServiceImpl.answer = answer.getAnswer();
						String[] options = answer.getOptions();
						questionContent.append(question);
						questionContent.append("\n\n");
						questionContent.append(options[0]);
						questionContent.append("\n");
						questionContent.append(options[1]);
						questionContent.append("\n");
						questionContent.append(options[2]);
						questionContent.append("\n");
						questionContent.append(options[3]);
						
						wechatService.webwxsendmsg(wechatMeta, questionContent.toString(),WechatServiceImpl.groupId);
						
					}else if(answer.getQuestionType() == -1){
						String question = answer.getQuestion();
						WechatServiceImpl.question = question;
						WechatServiceImpl.answer = answer.getAnswer();
						wechatService.webwxsendmsg(wechatMeta, question,WechatServiceImpl.groupId);
					}
					
					//答题状态开启
					WechatServiceImpl.answerEnable = true;
					try {
						Thread.sleep(22 * 1000);//每题之间，间隔秒
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					WechatServiceImpl.answerEnable = false;
				}
				
				
				
				//结算答题结果并公布
				Set<String> nicks = new HashSet<String>();
				final Map<String, Integer> rightRes = new HashMap<String,Integer>();
				WechatServiceImpl.rightAnswers.forEach((nkName,answersCount) ->{
					nicks.add(nkName);
					rightRes.put(nkName,new Integer(answersCount.size()));
				});
				
				
				final Map<String, Integer> errorRes = new HashMap<String,Integer>();
				WechatServiceImpl.errorAnswers.forEach((nkName,answersCount) ->{
					nicks.add(nkName);
					errorRes.put(nkName,new Integer(answersCount.size()));
				});
				
				StringBuilder totalContent = new StringBuilder();
				totalContent.append("答题结束！[愉快]\n现在公布答题结果：\n\n");
				for(String nnkkName:nicks){
					int rightCount = rightRes.get(nnkkName)==null?0:rightRes.get(nnkkName).intValue();
					int errorCount = errorRes.get(nnkkName)==null?0:errorRes.get(nnkkName).intValue();
					int haveCount = rightCount + errorCount;
					totalContent.append("@");
					totalContent.append(nnkkName);
					totalContent.append(" \n");
					totalContent.append("答题数：");
					totalContent.append(haveCount);
					totalContent.append("\n");
					totalContent.append("正确数：");
					totalContent.append(rightCount);
					totalContent.append("\n");
					totalContent.append("错误数：");
					totalContent.append(errorCount);
					totalContent.append("\n");
					totalContent.append("未答数：");
					totalContent.append(total - haveCount);
					totalContent.append("\n");
					totalContent.append("正确率");
					totalContent.append(new BigDecimal((double)rightCount*100/haveCount).setScale(2, RoundingMode.UP).doubleValue());
					totalContent.append("%");
					totalContent.append("\n\n");
				}
				totalContent.append("希望大家再接再厉，我是平安小管家，我们明天见！[再见]");
				wechatService.webwxsendmsg(wechatMeta, totalContent.toString(),WechatServiceImpl.groupId);
			}
		}, "answer");
		answerThread.start();
	}
	
}
