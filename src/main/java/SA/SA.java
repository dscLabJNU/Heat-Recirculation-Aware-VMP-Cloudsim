package SA;

import Jama.Matrix;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * @author ztq
 * @create 2021-10-01 17:45
 */
public class SA {
    private static List<Vm> vmlist;
    private static List<Cloudlet> cloudletList;

    //模拟退火
    static double initTemp = 1000;//初始温度
    static double finalTemp = 1.8739277 * Math.pow(10, -11);//终止温度
    static int perTempTimes = 100;//各温度下的迭代次数
    static double coolRate = 0.9;//降温速率
    static int nodeNum = 50;//节点个数
    static int taskNum = 800;//任务总量
    static double[][] DMatrix = new double[50][50];//干扰系数D矩阵
    static int[] taskAllocation = new int[nodeNum];

    public static void main(String[] args) throws Exception {
        int[] taskLength = new int[taskNum];


        DMatrix = loadDMatrix();//读取干扰系数矩阵D
        taskAllocation = initTaskAllocation();//生成随机初始任务分配
        outputPath(taskAllocation);//输出初始任务分配
        System.out.println("初始制冷系统提供气流温度：" + minCRACTemp(taskAllocation));
        System.out.println("初始总能耗：" + getTotalPower(nodeNum));
        taskReallocation();
        System.out.println("优化后制冷系统需要提供的温度：" + minCRACTemp(taskAllocation));
        System.out.println("优化后总能耗：" + getTotalPower(nodeNum));

        for (int i=0;i<taskNum;i++){
            taskLength[i]=100;
        }
        RunSimulation(taskLength,taskNum);//开始模拟仿真
    }

    private static double getTotalPower(int serverNum) {
        int serverPower=serverNum*2100;
        double coolingPower=serverPower/(0.0068*minCRACTemp(taskAllocation)*minCRACTemp(taskAllocation)+0.0008*minCRACTemp(taskAllocation)+0.458);
        double totalPower=serverPower+coolingPower;
        return totalPower;
    }
    //更新任务分配
    public static void taskReallocation() {
        int cycleTimes = 0;//循环总次数
        double perCycleTemp = initTemp;
        while (1 == 1) {
            perCycleTemp = perCycleTemp * coolRate;
            cycleTimes++;
            if (perCycleTemp == finalTemp) {
                break;
            }
            if (perCycleTemp < finalTemp) {
                break;
            }
        }
        double[][] cycleTemp = new double[cycleTimes][1];//记录SA各个温度下的Tsup
        int[][] track = new int[cycleTimes][nodeNum];//记录SA各个温度的最优路径

        int count = 0;
        int[] newTaskAllocation = new int[nodeNum];//新的任务分配
        while (initTemp > finalTemp) {
            double[][] trackAndTemp = new double[perTempTimes][nodeNum + 1];//记录SA当前温度下的路径和Tsup
            //当前SA温度下迭代
            for (int i = 0; i < perTempTimes; i++) {
                newTaskAllocation = newAnswer(taskAllocation);//产生新解
                metropolis(newTaskAllocation, initTemp);//概率接受新解
                for (int j = 0; j < taskAllocation.length; j++) {
                    trackAndTemp[i][j] = taskAllocation[j];
                }
                trackAndTemp[i][nodeNum] = minCRACTemp(taskAllocation);
            }
            //获得此SA温度下最优解
            double maxTemp = trackAndTemp[0][nodeNum];
            int maxTempIndex = 0;
            for (int i = 0; i < perTempTimes; i++) {
                if (trackAndTemp[i][nodeNum] > maxTemp) {
                    maxTemp = trackAndTemp[i][nodeNum];
                    maxTempIndex = i;
                }
            }
            if (count == 0 || maxTemp > cycleTemp[count - 1][0]) {
                cycleTemp[count][0] = maxTemp;
            } else {
                cycleTemp[count][0] = cycleTemp[count - 1][0];
            }
            for (int i = 0; i < nodeNum; i++) {
                track[count][i] = (int) trackAndTemp[maxTempIndex][i];
            }
            count++;
            initTemp = initTemp * coolRate;
        }
        taskAllocation = track[cycleTimes - 1].clone();
        outputPath(taskAllocation);

    }

    private static void metropolis(int[] newTaskAllocation, double initTemp) {
        double taskTsup = minCRACTemp(taskAllocation);
        double newTaskTsup = minCRACTemp(newTaskAllocation);
        double diff = taskTsup - newTaskTsup;
        if (diff < 0) {
            taskAllocation = newTaskAllocation.clone();
        } else if (Math.pow(Math.E, -1 * diff / initTemp) > Math.random()) {
            taskAllocation = newTaskAllocation.clone();
        }
    }

    private static int[] newAnswer(int[] taskAllocation) {
        int[] newTaskAllocation = (int[]) taskAllocation.clone();

        int[] changePoint = new int[2];
        changePoint[0] = (int) (Math.random() * nodeNum);
        changePoint[1] = (int) (Math.random() * nodeNum);

        int t = 0;
        t = newTaskAllocation[changePoint[0]];
        newTaskAllocation[changePoint[0]] = newTaskAllocation[changePoint[1]];
        newTaskAllocation[changePoint[1]] = t;
        return newTaskAllocation;
    }

    //初始任务分配
    public static int[] initTaskAllocation() {
        int[] taskAllocation = new int[nodeNum];
        int sum = 0;
        int allocationTask = 0, remainTask = 0;
        for (int i = 0; i < nodeNum; i++) {
            taskAllocation[i] = (int) (Math.random() * 10 + 1);
            sum += taskAllocation[i];
        }
        for (int i = 0; i < nodeNum; i++) {
            taskAllocation[i] = (int) ((taskAllocation[i] * taskNum) / sum);
            allocationTask += taskAllocation[i];
        }
        remainTask = taskNum - allocationTask;//未分配的任务
        taskAllocation[(int) (Math.random() * 50)] += remainTask;

        boolean flag = true;
        while (flag) {
            //使任务分配不超过最大负载
            int max = taskAllocation[0];
            int maxIndex = 0;
            int min = taskAllocation[0];
            int minIndex = 0;
            for (int i = 0; i < nodeNum; i++) {
                if (taskAllocation[i] > max) {
                    max = taskAllocation[i];
                    maxIndex = i;
                }
                if (taskAllocation[i] < min) {
                    min = taskAllocation[i];
                    minIndex = i;
                }
            }
            if (max > 40) {
                taskAllocation[maxIndex] = 40;
                taskAllocation[minIndex] = min + max - 40;
            } else {
                flag = false;
            }
        }
//        int totalTask=0;
//        for (int i=0;i<taskAllocation.length;i++){
//            totalTask+=taskAllocation[i];
//        }
//        System.out.println("最终实际分配任务总数："+totalTask);
        return taskAllocation;
    }

    //载入D矩阵
    public static double[][] loadDMatrix() throws Exception {
        double DMatrix[][] = new double[50][50];
        int i = 0;
        FileReader file = new FileReader("src/file/DMatrix.txt");
        BufferedReader br = new BufferedReader(file);
        String s = null;
        while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
            String[] lineData = new String[50];
            int j = 0;
            lineData = s.split(" ");
            for (String data : lineData) {
                DMatrix[i][j] = Double.parseDouble(data);
                j++;
            }
            i++;
        }

        return DMatrix;
    }

    //计算空调提供温度值
    public static double minCRACTemp(int[] taskAllocation) {
        double[][] tempMatrixArray = new double[1][nodeNum];
        for (int i = 0; i < tempMatrixArray[0].length; i++) {
            tempMatrixArray[0][i] = 1;
        }
        Matrix Db;
        Matrix tempMatrix = new Matrix(tempMatrixArray);
        Matrix dMatrix = new Matrix(DMatrix);
        Db = tempMatrix.times(2020).times(dMatrix.transpose());

        Matrix Tin = tempMatrix.times(26);//入口温度
        double[][] taskAllocationMatrixArray = new double[1][nodeNum];
        for (int i = 0; i < taskAllocation.length; i++) {
            taskAllocationMatrixArray[0][i] = taskAllocation[i];
        }
        Matrix taskAllocationMatrix = new Matrix(taskAllocationMatrixArray);
        //计算空调给各个节点应该提供的温度
        Matrix Tsup = Tin.minus(Db).minus(taskAllocationMatrix.times(50).times(dMatrix.transpose()));
        double[][] TsupArray = Tsup.getArray();
        double min = TsupArray[0][0];
        for (int i = 0; i < TsupArray[0].length; i++) {
            if (TsupArray[0][i] < min) {
                min = TsupArray[0][i];
            }
        }
        return min;
    }

    //输出分配方案
    public static void outputPath(int[] taskAllocation) {
        for (int i = 0; i < taskAllocation.length; i++) {
            System.out.print(taskAllocation[i]);
            if ((i + 1) % 5 == 0) {
                System.out.println("");
            } else {
                System.out.print("-->");
            }
        }
    }

    public static void RunSimulation(int[] taskLength, int taskNum) {
        System.out.println("Starting to run simulations...");

        try {
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            //下面创建的datacenter是用来运行任务的物理硬件，是必需的，否则不能运行。它被创建之后看似没有调用，好像没啥用，其实DataCenter构造函数把它与CloudSim类进行了绑定，所以能够发挥作用
            @SuppressWarnings("unused")
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // #3 step: Create Broker 用来分配任务到虚拟机
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // #4 step: Create one virtual machine
            createVms(brokerId);

            // submit vm list to the broker
            broker.submitVmList(vmlist);

            // #5 step: Create cloudlets
            createCloudlets(brokerId, taskLength);

            broker.submitCloudletList(cloudletList);

//            DCBroker提供了自带的Cloudlet列表->VM列表的绑定逻辑，用于将剩下的Cloudlet绑定到VM上（编程者未绑定的那些）
//            主要逻辑是检查列表中每个Cloudlet是否已经有主，如果没有，则使用轮转法完成分配
            int sum=0;
            for(int i=0;i<nodeNum;i++)
            {
                System.out.println("虚拟机"+i+"任务数量："+taskAllocation[i]);
                for (int j=0;j<taskAllocation[i];j++){
                    //下面的两行代码用于把任务绑定到指定的虚拟机上，两行代码效果是一样的
                    //如果需要用自己实现的算法来进行资源调度，则可以在算法中动态调用DataCenterBroker.bindCloudletToVm()方法或者Cloudlet.setVmId()方法

                    broker.bindCloudletToVm(cloudletList.get(j+sum).getCloudletId(),i);
                    System.out.println("虚拟机"+i+"任务id："+cloudletList.get(j+sum).getCloudletId());
//                cloudletList.get(i).setVmId(vm1.getId());
                }
                sum+=taskAllocation[i];
            }
            //Deleted codes.
            //bindAllTaskToSameVM(cloudletList,vm1.getId());

            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            for (Vm vm : vmlist) {
                System.out.println(String.format("vm id= %s ,mips = %s ", vm.getId(), vm.getMips()));
            }

            printCloudletList(newList);
            System.out.println("CloudSim simulation is finished!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<Host>();
        List<Pe> peList = new ArrayList<Pe>();

        //创建五个cpu,mips为cpu的处理速度
        int mips = 5000;
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        mips = 2500;
        peList.add(new Pe(1, new PeProvisionerSimple(mips))); // need to store

        mips = 2500;
        peList.add(new Pe(2, new PeProvisionerSimple(mips))); // need to store

        mips = 1500;
        peList.add(new Pe(3, new PeProvisionerSimple(mips))); // need to store

        mips = 1000;
        peList.add(new Pe(4, new PeProvisionerSimple(mips))); // need to store

        for (int i = 0; i < nodeNum; i++) {
            int hostId = i;
            int ram = 512; // host memory (MB)
            long storage = 10000000; // host storage
            int bw = 10000;

            hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw), storage, peList,
                    new VmSchedulerTimeShared(peList)));
        }

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processors in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.001; // the cost of using bw in this resource

        //we are not adding SAN devices by now
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 0);//VmAllocationPolicySimple虚拟机分配策略
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static DatacenterBroker createBroker() {

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static void createVms(int brokerId) {
        // #4 step: Create one virtual machine
        vmlist = new ArrayList<Vm>();

        // VM description
        long size = 10000; // image size (MB)
        int ram = 512; // vm memory (MB)
        long bw = 1000;
        int pesNumber = 1; // number of cpus
        String vmm = "Xen"; // VMM name


        //所有虚拟机的mips之和不能超过datacenter中定义的主机的物理cpu的mips之和，而虚拟cpu的mips的最大值也不能超过物理cpu的最大值，否则虚拟机将创建失败。

        for (int i = 0; i < nodeNum; i++) {
            double mips = 2500;//mips是虚拟机的cpu处理速度，cloudlet的length/虚拟机mips=任务执行所需时间
            Vm vm1 = new Vm(i, brokerId, mips, pesNumber, ram, bw, size,
                    vmm, new CloudletSchedulerSpaceShared());
            // add the VMs to the vmList
            vmlist.add(vm1);
        }


    }

    private static void createCloudlets(int brokerId, int[] taskLength) {
        // #5 step: Create cloudlets
        cloudletList = new ArrayList<Cloudlet>();

        // Cloudlet properties
//        int id = 0;
        int pesNumber = 1;

        long fileSize = 100;
        long outputSize = 10000000;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < taskNum; i++) {
            //Cloudlet构造函数的一个参数为任务id，第二个参数为任务长度(指令数量)
            Cloudlet task = new Cloudlet(i, taskLength[i], pesNumber, fileSize,
                    outputSize, utilizationModel, utilizationModel,
                    utilizationModel);
            task.setUserId(brokerId);

            cloudletList.add(task);
        }

    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "CloudletLength" + indent + "Time"
                + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                System.out.println(indent + indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + cloudlet.getCloudletLength()
                        + indent + indent + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()) + indent
                        + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}
