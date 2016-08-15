# chinabike-plugins-MultiImagesPicker
多图选择

MultiImagesPicker.getPictures(onSuccess, onFail, params);
注：params可选<br/>
<br/>
使用方法：<br/>
<code>
navigator.MultiImagesPicker.getPictures(function(result) {
    alert(result);
}, function(err) {
    alert(err);
}, { max : 9, width : 0, height : 0, quality : 100 });
</code>
