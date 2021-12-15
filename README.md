<h1 align="center">WorldView-Android</h1>
<div align="center">

![](https://img.shields.io/badge/android-4.0%2B-blue)
[![](https://jitpack.io/v/ftmtshuashua/WorldView-Android.svg)](https://jitpack.io/#ftmtshuashua/WorldView-Android)
[![License Apache2.0](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0.html)



</div>

帮助开发者快速的开发出高质量的自定义View组件

# 概念

> 世界 :世界的大小是无限的,View自身的大小是固定的. View的大小可以理解为世界的可视区域.

> 相机机位 :世界的可视区域,通常就是指View本身的大小所能看到的内容

# WorldView

继承WorldView实现自己的世界

```
public class MyView extends WorldView{
    @Override
    protected void onMeasureWorldSize(WorldParameter world, int width, int height) {
        //默认的世界大小等于View在布局中的大小
        super.onMeasureWorldSize(world,width,height);
        //自定义时计算并设置世界的大小
        world.setWorldSize(myWidth, myHeight);
    }
    
    @Override
    protected void onDrawWorld(Canvas canvas) {
        //在这里将整个世界绘制出来
    }
    
    @Override
    protected void onDown(float x, float y) {
        //当用户点击时触发
    }
    
    @Override
    protected void onPressStateChange(boolean press, float x, float y) {
        //用户触摸状态改变时回调
    }
    
    @Override
    protected void onSingleTap(float x, float y) {
        //用户单击事件，注意 用户双击时会触发两次回调,如果需要同时检测双击和单击事件请使用[onSingleTapByDouble()]
    }
    
    @Override
    protected void onSingleTapByDouble(float x, float y){
        //过滤掉双击事件之后的 用户单击事件
    }
    
    @Override
    protected void onDoubleTap(float x, float y) {
        //用户双击事件
    }
    
    @Override
    protected void onLongPress(float x, float y) {
        //长按事件 ,如需使用长按事件 需要启用它[setLongPress(true)]
    }
    
    @Override
    protected void onFling(float velocityX, float velocityY) {
        //推拽并甩动
    }
    
    @Override
    protected void onScroll(int oldX, int oldY, int newX, int newY, int distanceX, int distanceY) {
        //用户拖动
    }
}
```

# WorldBufferView

在WorldView的基础上提供二级缓存,提升性能

```
public class MyView extends WorldBufferView{
    @Override
    protected void onDrawWorldLauncher(Canvas canvas) {
        //当 onDrawWorldBuffer() 未绘制完成时候回调
    }
    
    @Override
    protected void onDrawWorldBuffer(Canvas canvas) {
        //缓冲部分，该部分绘制内容只会调用一次以提高绘制性能
        //如需刷新请调用 [setBufferReset()]
    }
    
    @Override
    protected void onDrawWorldAnimation(Canvas canvas) {
        //需要动态绘制的部分
    }

}
```

# 接入

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

```
	dependencies {
	        implementation 'com.github.ftmtshuashua:WorldView-Android:version'
	}
```