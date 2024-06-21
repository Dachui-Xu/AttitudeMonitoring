# 项目实现进展

项目需求见：[requestments.md](requirements.md)

传感器采用TENG，一种柔性传感，用于获取颈部的姿态，如低头，抬头，转头等，不同姿态下维持特定时间会进行报警。

> 2024-06-21：完成蓝牙通信，数据获取，多折线图绘制和日志等功能

# UML

![image-8e96a3275b5c8866dcf57be3cba057fe.png](https://t.tutu.to/img/vZzih)

# 模块部分

绘制折线图：采用API-->[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) ，由于要用动态获取，数据量大且要切换Actually，所以采用了MVVM模式。

**Model**，创建动态折线图类[LineChartHander](AttitudeMonitoring/app/src/main/java/com/example/attitudemonitoring/handler/LineChartHandler.kt)，再用[MultiLineChartHandler](AttitudeMonitoring/app/src/main/java/com/example/attitudemonitoring/handler/MultiLineChartHandler.kt)将创建多个折线图，并将折线图与数据绑定。

**ViewModel**，用于管理折线图和传感器数据，避免在切换页面时数据丢失，在多折线图，数据量大时造成卡顿，[MultipleLineChartsViewModel](AttitudeMonitoring/app/src/main/java/com/example/attitudemonitoring/viewModel/MultipleLineChartsViewModel.kt)继承了Android的ViewModel框架，用[ViewModelFactory](AttitudeMonitoring/app/src/main/java/com/example/attitudemonitoring/viewModel/ViewModelFactory.kt)即工厂方法的模式进行创建。

**View**：视图，在[LineChartsView.kt](AttitudeMonitoring/app/src/main/java/com/example/attitudemonitoring/ui/widgets/LineChartsView.kt)文件中，函数`LineChartView`创建1个折线图，`MultipleLineChartsView`创建了8个折线图，可以开始，停止，清空，重启，最后数据还可以导出到社交媒体中。

------

蓝牙部分：采用API-->[BluetoothClient: Android蓝牙客户端)](https://github.com/zhzc0x/BluetoothClient)

# 参考

见文件夹`withGPT`

[Jetpack ViewModel (源码分析) 面试](https://juejin.cn/post/7379823758420148276)
