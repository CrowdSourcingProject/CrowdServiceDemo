package sg.edu.sutd.ws;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jws.WebService;

import com.google.gson.Gson;
import edu.fudan.se.crowdservice.plan.Request;
import sutd.edu.sg.CrowdOptimizationResult;
import sutd.edu.sg.CrowdServiceProxy;
import sutd.edu.sg.CrowdWorker;

import com.microsoft.schemas._2003._10.Serialization.Arrays.ArrayOfKeyValueOfstringArrayOfCrowdWorker8Qgdyvm9KeyValueOfstringArrayOfCrowdWorker8Qgdyvm9;
import com.microsoft.schemas._2003._10.Serialization.Arrays.ArrayOfKeyValueOfstringintKeyValueOfstringint;

import edu.fudan.se.bean.AgentInfo;
import edu.fudan.se.dbopration.SelectOnlineAgentInfoOperator;

@WebService(endpointInterface = "sg.edu.sutd.ws.GlobalOptimization")
public class GlobalOptimizationImpl implements GlobalOptimization {

	final public static double PACE_SPEED = 1.1;


//	private HashMap<String, BaseVariable> mapping = new HashMap<String, BaseVariable>();
	public GlobalOptimizationImpl(){

	}
	//locationStr的格式类似于{[InspectSiteService:latitude:longitude]},
	//即,针对于全局优化的时候，对于单个用户的某一次执行请求的时候，
	
	@Override
	public String globalOptimize(String content){

        Request request = new Gson().fromJson(content, Request.class);
		long globalTimeConstraint = request.getGlobalCost();
		double globalCostConstraint = request.getGlobalCost();
		Map<String, Integer> resultNumsMap = request.getResultNumbers();
		String[] serviceSequence = request.getServiceSequence();
		String templateName = request.getTemplateName();

		String xml = "";
		if(serviceSequence.length == 1)
			xml = step1;
		if(serviceSequence.length == 1)
			xml = step2;
		if(serviceSequence.length == 1)
			xml = step3;


		int idCounter = 0;
//		int locationNum = locationStr.length;
//		HashMap<String, String> locationMap = new HashMap<String, String>();
//		for(int i = 0; i < locationNum ; i++){
//			int startLocation = locationStr[i].indexOf(":");
//			String csName = locationStr[i].substring(0, startLocation);//crowdService Name
//			String correspondingLocation = locationStr[i].substring(startLocation+1, locationStr[i].length());
//			locationMap.put(csName, correspondingLocation);
//		}
	
		//=======================================
		ArrayOfKeyValueOfstringintKeyValueOfstringint[] resultNums = {};
		ArrayList<ArrayOfKeyValueOfstringintKeyValueOfstringint> al = new ArrayList<ArrayOfKeyValueOfstringintKeyValueOfstringint>();

		for(Map.Entry<String,Integer> entry : resultNumsMap.entrySet()){
			al.add(new ArrayOfKeyValueOfstringintKeyValueOfstringint(entry.getKey(),entry.getValue()));
		}



		resultNums = al.toArray(resultNums);
		int resultNumLen =resultNums.length;
		//=======================================
		
		ArrayList<AgentInfo> agentInfos = new SelectOnlineAgentInfoOperator().getResult();
		CrowdWorker[] workers = new CrowdWorker[agentInfos.size()];
		ArrayOfKeyValueOfstringArrayOfCrowdWorker8Qgdyvm9KeyValueOfstringArrayOfCrowdWorker8Qgdyvm9[] aov = 
				new ArrayOfKeyValueOfstringArrayOfCrowdWorker8Qgdyvm9KeyValueOfstringArrayOfCrowdWorker8Qgdyvm9[resultNumLen];
		
		for(int i = 0;i<resultNumLen;i++){
			    String crowdServiceName = resultNums[i].getKey();
//				BaseVariable bv = mapping.get(crowdServiceName);
//					if(bv == null) return null;
				double baseCostConstant = BaseVariable.getCorrespondingBaseCostConst(crowdServiceName);
				double baseTimeConstant = BaseVariable.getCorrespondingBaseTimeConst(crowdServiceName);
			    double coefficient = BaseVariable.getCorrespondingCoefficient(crowdServiceName);
//			    String correspondingLocation = locationMap.get(crowdServiceName);
			    
				for(int j = 0; j < agentInfos.size(); j++){
					
					AgentInfo agentInfo = agentInfos.get(j);
//					double distance = correspondingLocation ==
//							null ? 0 : getShortDistance(correspondingLocation,agentInfo.latitude+":"+agentInfo.longitude);
//					double cost = baseCostConstant + coefficient <= 1e-6 ? 0 : coefficient*distance;
//					long responseTime =  (long)(baseTimeConstant + distance / PACE_SPEED);

					double cost = 5 + 4*(Math.random()-0.5);//TODO
					long responseTime = 3000 + (long)(2000*(Math.random()-0.5));

					CrowdWorker worker = new CrowdWorker(cost,idCounter++,agentInfos.get(i).reputation,responseTime,false);		
					workers[j] = worker;
				}
			aov[i] = new ArrayOfKeyValueOfstringArrayOfCrowdWorker8Qgdyvm9KeyValueOfstringArrayOfCrowdWorker8Qgdyvm9(crowdServiceName,workers);
		}
		
		try {
			CrowdOptimizationResult ret = new CrowdServiceProxy().globalOptimize(
					xml, 
					globalTimeConstraint,
					globalCostConstraint, 
					aov, 
					resultNums, 
					400);
			if(ret != null){
				double totalReliability = ret.getTotalReliability();
				ArrayOfKeyValueOfstringArrayOfCrowdWorker8Qgdyvm9KeyValueOfstringArrayOfCrowdWorker8Qgdyvm9 seletedWorker = 
						ret.getCrowdServiceSelection()[0];
				CrowdWorker[] cw = seletedWorker.getValue();
				long partTime = 0;
				double partCost = 0;
				for(CrowdWorker tmp : cw){
					partTime += tmp.getResponseTime();
					partCost += tmp.getCost();
				}
				return "totalReliability:" + totalReliability + ",partTime:" + partTime + ",partCost:" + partCost;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * 根据位置中的经纬度计算出距离

	 * @return 两个位置之间的距离
	 */
	private static double getShortDistance(String location1, String location2) {

		double lat1 = Double.parseDouble(location1.split(":")[0]);
		double lon1 = Double.parseDouble(location1.split(":")[1]);

		double lat2 = Double.parseDouble(location2.split(":")[0]);
		double lon2 = Double.parseDouble(location2.split(":")[1]);

		double a, b, R;
		R = 6378137; // 地球半径
		lat1 = lat1 * Math.PI / 180.0;
		lat2 = lat2 * Math.PI / 180.0;
		a = lat1 - lat2;
		b = (lon1 - lon2) * Math.PI / 180.0;
		double d;
		double sa2, sb2;
		sa2 = Math.sin(a / 2.0);
		sb2 = Math.sin(b / 2.0);
		d = 2
				* R
				* Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1)
						* Math.cos(lat2) * sb2 * sb2));
		return d;
	}

	static String step1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<process name=\"Intermediary\"\n" +
			"xmlns=\"http://docs.oasis-open.org/wsbpel/2.0/process/executable\"\n" +
			"targetNamespace=\"http://enterprise.netbeans.org/bpel/BpelModule/Initiator\"\n" +
			"xmlns:ext=\"http://pat.comp.nus.edu.sg/BPEL\"\n" +
			"xmlns:bpel=\"http://ultraBpel/\">  \n" +
			"<sequence>\n" +
			"\t<receive ext:responseTimeTag=\"customer\" partnerLink=\"customer\" operation=\"BuySecondhandItem\" variable=\"var\"  createInstance=\"yes\"/>\n" +
			"\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"300;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t<flow>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"20;0;0.9\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:crowdServiceName=\"edu.fudan.se.crowdservice.siteinspection.InspectSiteService\" partnerLink=\"PBS1\" operation=\"requestMS1\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\t\t\n" +
			"\t</flow>\n" +
			"\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"120;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t<if ext:ifProb=\"0.5;0.5\">\n" +
			"\t\t<condition>ContinueYes</condition>\n" +
			"\t\t<sequence>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:crowdServiceName=\"edu.fudan.se.crowdservice.priceassessment.AssessPriceService\" partnerLink=\"PBS1\" operation=\"requestMS1\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t<if ext:ifProb=\"0.5;0.5\">\n" +
			"\t\t\t<condition>PriceOK</condition>\n" +
			"\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"60;0;0.9\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t<else>\n" +
			"\t\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"0;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t</else>\n" +
			"\t\t</if>\n" +
			"\t\t</sequence>\n" +
			"\t\t<else>\n" +
			"\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"0;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t</else>\n" +
			"\t</if>\t\n" +
			"\t<reply partnerLink=\"customer\" bpel:ReplyUser=\"true\" operation=\"BuySecondhandItem\" variable=\"result\"/>\n" +
			"</sequence>\n" +
			"</process>";

	static String step2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<process name=\"Intermediary\"\n" +
			"xmlns=\"http://docs.oasis-open.org/wsbpel/2.0/process/executable\"\n" +
			"targetNamespace=\"http://enterprise.netbeans.org/bpel/BpelModule/Initiator\"\n" +
			"xmlns:ext=\"http://pat.comp.nus.edu.sg/BPEL\"\n" +
			"xmlns:bpel=\"http://ultraBpel/\">  \n" +
			"<sequence>\n" +
			"\t<receive ext:responseTimeTag=\"customer\" partnerLink=\"customer\" operation=\"BuySecondhandItem\" variable=\"var\"  createInstance=\"yes\"/>\n" +
			"\t<flow>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"20;0;0.9\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:crowdServiceName=\"edu.fudan.se.crowdservice.siteinspection.InspectSiteService\" partnerLink=\"PBS1\" operation=\"requestMS1\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\t\t\n" +
			"\t</flow>\n" +
			"\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"120;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t<if ext:ifProb=\"0.5;0.5\">\n" +
			"\t\t<condition>ContinueYes</condition>\n" +
			"\t\t<sequence>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:crowdServiceName=\"edu.fudan.se.crowdservice.priceassessment.AssessPriceService\" partnerLink=\"PBS1\" operation=\"requestMS1\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t<if ext:ifProb=\"0.5;0.5\">\n" +
			"\t\t\t<condition>PriceOK</condition>\n" +
			"\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"60;0;0.9\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t<else>\n" +
			"\t\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"0;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t</else>\n" +
			"\t\t</if>\n" +
			"\t\t</sequence>\n" +
			"\t\t<else>\n" +
			"\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"0;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t</else>\n" +
			"\t</if>\t\n" +
			"\t<reply partnerLink=\"customer\" bpel:ReplyUser=\"true\" operation=\"BuySecondhandItem\" variable=\"result\"/>\n" +
			"</sequence>\n" +
			"</process>";


	static String step3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<process name=\"Intermediary\"\n" +
			"xmlns=\"http://docs.oasis-open.org/wsbpel/2.0/process/executable\"\n" +
			"targetNamespace=\"http://enterprise.netbeans.org/bpel/BpelModule/Initiator\"\n" +
			"xmlns:ext=\"http://pat.comp.nus.edu.sg/BPEL\"\n" +
			"xmlns:bpel=\"http://ultraBpel/\">  \n" +
			"<sequence>\n" +
			"\t<receive ext:responseTimeTag=\"customer\" partnerLink=\"customer\" operation=\"BuySecondhandItem\" variable=\"var\"  createInstance=\"yes\"/>\n" +
			"\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:crowdServiceName=\"edu.fudan.se.crowdservice.priceassessment.AssessPriceService\" partnerLink=\"PBS1\" operation=\"requestMS1\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t<if ext:ifProb=\"0.5;0.5\">\n" +
			"\t\t\t<condition>PriceOK</condition>\n" +
			"\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"60;0;0.9\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t<else>\n" +
			"\t\t\t\t<invoke bpel:BPELCategory=\"GeneralService\" ext:QoS=\"0;0;1\" partnerLink=\"PBS\" operation=\"requestMS\" inputVariable=\"MSInfo\"  outputVariable=\"ISOutput\"/>\n" +
			"\t\t\t</else>\n" +
			"\t\t</if>\n" +
			"\t<reply partnerLink=\"customer\" bpel:ReplyUser=\"true\" operation=\"BuySecondhandItem\" variable=\"result\"/>\n" +
			"</sequence>\n" +
			"</process>";
	
}