var exec = require('cordova/exec');
    
var MultiImagesPicker = {};

/**
 * @param onSuccess 图片选取成功的回调函数
 * @param onFail 失败的回调函数
 * @param params 可选参数：{max:最多选取几张图片(默认9张),width:图片宽度,height:图片高度,quality:图片质量}
 */
MultiImagesPicker.getPictures = function(onSuccess, onFail, params) {
    exec(onSuccess, onFail, "MultiImagesPicker", "getPictures", [params]);
}
    
module.exports = MultiImagesPicker;
