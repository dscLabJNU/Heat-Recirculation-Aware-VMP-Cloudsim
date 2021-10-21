# The SABA algorithm was proposed in the paper "Towards Heat-Recirculation-Aware Virtual Machine Placement in Data Centers"（Hao Feng, Y. Deng, Y. Zhou and G. Min, "Towards Heat-Recirculation-Aware Virtual Machine Placement in Data Centers," in IEEE Transactions on Network and Service Management, online, doi: 10.1109/TNSM.2021.3120295. ）.
## 1. Introduction
The SABA algorithm is proposed to reduce the power of data centers by using the heat-recirculation-aware virtual machine placement.
## 2. Environment
1. idea 2020
2. CloudSim 3.0.3
3. jama 1.0.3 （matrix operations）
## 3. SA and SABA algorithm
methods：
```java
loadDMatrix()

initTaskAllocation();//Generate a random VMP result.

minCRACTemp(int[] taskAllocation)//taskAllocation[]Is the task assignment matrix

getTotalPower(int serverNum)//Calculate the total energy consumption, serverNum is the total number of activated servers.

taskReallocation()//Use simulated annealing algorithm to generate new task assignments.

newAnswer(int[] taskAllocation)//Generate new solutions.

metropolis(int[] newTaskAllocation, double initTemp)//

outputPath(int[] taskAllocation)//Output task allocation status, taskAllocation[] is the task allocation matrix

RunSimulation(int[] taskLength, int taskNum)//Start simulation, taskLength[] task length is set to 100

createDatacenter(String name)//Create a data center and set up 50 servers.

createBroker()//

createVms(int brokerId)//Create virtual machines.

createCloudlets(int brokerId, int[] taskLength)//Set up the number of tasks.

```
## 4. Run
1. Import idea after downloading (File->Open->Select the downloaded project file)
2. Right-click the CloudSim jar package in the jars folder, and click Add As Library
3. Run the classes in the SA folder and SABA folder respectively.
