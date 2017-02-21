package me.biezhi.wechat;

import com.blade.kit.base.Config;

import me.biezhi.wechat.listener.WechatListener;

public class Application {
	
	public static void main(String[] args) {
		if(args.length!=0){
			WechatListener.questionFilePath = args[args.length-1];
		}
		try {
			
			Constant.config = Config.load("classpath:config.properties");
			WechatRobot wechatRobot = new WechatRobot();
			wechatRobot.showQrCode();
			while(!Constant.HTTP_OK.equals(wechatRobot.waitForLogin())){
				Thread.sleep(2000);
			}
			wechatRobot.closeQrWindow();
			wechatRobot.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}