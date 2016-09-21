# chinabike-plugins-MultiImagesPicker
android多图选择(带目录)
##主要功能
- 选择获取多张图片地址

##安装要求
- Cordova Version >=3.5
- Cordova-Android >=4.0
- Cordova-iOS >=4.0

##安装
1. 命令行运行      ```cordova plugin add https://github.com/giantss/com.giants.multiimagespicker.git```  
2. 命令行运行 cordova build --device    

##注意事项					        	
1. 这个插件要求cordova-android 的版本 >=4.0,推荐使用 cordova  5.0.0 或更高的版本，因为从cordova 5.0 开始cordova-android 4.0 是默认使用的android版本
2. 请在cordova的deviceready事件触发以后再调用本插件！！！	
3. 在AndroidManifest.xml里面的application标签里加上 <code>android:name="com.chinabike.plugins.mip.AppContext"</code>

##使用方式  

```Javascript
navigator.MultiImagesPicker.getPictures(function(result) {
    alert(result);
}, function(err) {
    alert(err);
}, { max : 9, width : 0, height : 0, quality : 100 });
					
```	 

