# CloudSim 虚拟机放置SABA算法已发表于Towards Heat-Recirculation-Aware Virtual Machine Placement in Data Centers论文（Hao Feng, Y. Deng, Y. Zhou and G. Min, "Towards Heat-Recirculation-Aware Virtual Machine Placement in Data Centers," in IEEE Transactions on Network and Service Management, online, doi: 10.1109/TNSM.2021.3120295. ）。
## 1. 文件列表
## 1. 介绍
使用合适的任务调度策略来降低数据中心的能耗。
## 2. 环境
1. idea 2020
2. CloudSim 3.0.3
3. jama 1.0.3 （矩阵运算）
## 3. SA算法
降低CRAC的能耗。
有如下方法：
```java
loadDMatrix()//读取干扰系数矩阵D

initTaskAllocation();//生成随机初始任务分配

minCRACTemp(int[] taskAllocation)//计算空调提供温度值,taskAllocation[]是任务分配矩阵

getTotalPower(int serverNum)//计算总能耗，serverNum是开启的服务器总数量

taskReallocation()//使用模拟退火算法生成新的任务分配

newAnswer(int[] taskAllocation)//产生新解

metropolis(int[] newTaskAllocation, double initTemp)//以一定的概率接受新解，newTaskAllocation[]产生的新解，initTemp初始任务分配CRAC提供的温度

outputPath(int[] taskAllocation)//输出任务分配情况,taskAllocation[]是任务分配矩阵

RunSimulation(int[] taskLength, int taskNum)//开始仿真,taskLength[]任务长度均设为100

createDatacenter(String name)//创建数据中心，设置50台主机

createBroker()//创建代理

createVms(int brokerId)//创建50个虚拟机

createCloudlets(int brokerId, int[] taskLength)//设置800个任务

```
## 4. SABA
与SA算法相比，减少了低负载情况下开启的服务器数量，达到低负载情况下降低能耗的效果。
包含的方法和SA算法中的一样，增加了计算开启服务器数量的方法：
```java
private static int serverNum(int taskNum) {  
    int serverNum = nodeNum;  
 if (taskNum <= 1300) {  
        serverNum = (int) Math.ceil(taskNum / 0.65 / 40);  //0.65是服务器平均使用率，40是单个节点最大负载
  }  
    return serverNum;  
}
```
## 5. 运行
1. 下载之后导入idea（File->Open->选择下载的项目文件）
2. 右键单击jars文件夹里面的CloudSim jar包，点击Add As Library
3. 分别运行SA文件夹和SABA文件夹里面的类即可。
