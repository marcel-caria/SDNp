import java.util.HashMap;
import java.util.LinkedList;


public class Histogramm {
	
	HashMap<String, int[]> matches_on_utilization_class;	// the actual histogram for each type of optimization
	HashMap<String, Integer> result_count;						// number of results per type of optimization
	HashMap<String, Integer> reconfigurations_in_OSPF;						// number of results per type of optimization
	HashMap<String, Integer> reconversion_hops;						// number of results per type of optimization
	HashMap<String, Double> total_cost;						// accumulated total cost for each type of optimization
	int classes_cnt;											// number of utilization classes
	int class_diff;
	int[] histogramm_class;										// histogramm_class[n] stores the utilization in parts per thousand of class n
	LinearFunction costFunc;
	
	
	public Histogramm(int link_count, int class_dif){
		this.matches_on_utilization_class = new HashMap<String, int[]>();
		this.result_count = new HashMap<String, Integer>();
		this.total_cost = new HashMap<String, Double>();
		this.class_diff = class_dif;
		
		classes_cnt = 0;
		int c = 0;
		while (c <= 2000){
			c += class_dif;
			classes_cnt++;
		}
		this.histogramm_class = new int[classes_cnt];
		c = 0;
		for (int n=0; n<classes_cnt; n++){
			this.histogramm_class[n] = c;
			c += class_dif;
		}
	}
	
	public Histogramm(int link_count, int class_dif, LinearFunction costFunc){
		this.costFunc = costFunc;
		this.matches_on_utilization_class = new HashMap<String, int[]>();
		this.result_count = new HashMap<String, Integer>();
		this.reconfigurations_in_OSPF = new HashMap<String, Integer>();
		this.reconversion_hops = new HashMap<String, Integer>();
		this.total_cost = new HashMap<String, Double>();
		this.class_diff = class_dif;
		
		classes_cnt = 0;
		int c = 0;
		while (c <= 10000){
			c += class_dif;
			classes_cnt++;
		}
		this.histogramm_class = new int[classes_cnt];
		c = 0;
		for (int n=0; n<classes_cnt; n++){
			this.histogramm_class[n] = c;
			c += class_dif;
		}
	}

	@Override
	public String toString(){
		String out = "UtilCl";
		LinkedList<String> names = new LinkedList<String>();
		names.addAll(matches_on_utilization_class.keySet());
		LinkedList<String> orderedNames = new LinkedList<String>();
		while (!names.isEmpty()){
			String topmostName = names.getFirst();
			for (String otherName : names)
				if (topmostName.compareTo(otherName) > 0)
					topmostName = otherName;
			orderedNames.add(topmostName);
			names.remove(topmostName);
		}
		
		// names in the header
		for (int t=0; t<orderedNames.size(); t++)
			out += "\t" + orderedNames.get(t);
		out += "\r\n";
		
		// determine last entry
		int lastclass = 0;
		for (int c=0; c<this.classes_cnt; c++)
			if (lastclass < c)
				for (int t=0; t<orderedNames.size(); t++)
					if (matches_on_utilization_class.get(orderedNames.get(t))[c] > 0)
						lastclass = c;
		
		// results in each line
		for (int c=0; c<=lastclass+1; c++){
			out += this.histogramm_class[c];
			for (int t=0; t<orderedNames.size(); t++)
				if (matches_on_utilization_class.get(orderedNames.get(t))[c] == 0)
					out += "\t.";
				else
					out += "\t" + matches_on_utilization_class.get(orderedNames.get(t))[c];
			out += "\r\n";
		}
		
		// final line with aggregated information
		out += "\r\n";
		for (int t=0; t<orderedNames.size(); t++){
			out += result_count.get(orderedNames.get(t)) + " results for " + orderedNames.get(t);
			out += " with total cost:" + String.format("%.2f", total_cost.get(orderedNames.get(t)));
			if (reconfigurations_in_OSPF.keySet().contains(orderedNames.get(t)))
				out += " and " + reconfigurations_in_OSPF.get(orderedNames.get(t)) + " OSPF reconfigurations";
			if (reconversion_hops.keySet().contains(orderedNames.get(t)))
				out += " and " + reconversion_hops.get(orderedNames.get(t)) + " flooding hops";
			out += "\r\n";
		}
		out += "\r\n";
		
		return out;
	}
	
	public void add(String result_name, int[] link_utilization) {
		
		// if this is the first result of this kind:
		if (!matches_on_utilization_class.containsKey(result_name)){
			matches_on_utilization_class.put(result_name, new int[classes_cnt]);
			result_count.put(result_name, 0);
			total_cost.put(result_name, 0.0);
		}
		
		// adding links to the histogram
		for (int lu : link_utilization){
			int cl = 0;
			while (lu >= this.histogramm_class[cl])
				cl++;
			matches_on_utilization_class.get(result_name)[cl-1]++;
		}
		
		// calculating cost
		double accumulated_cost = 0.0;
		for (int lu : link_utilization)
			accumulated_cost += this.costFunc.calculate(0.001 * lu);
		
		int new_result_count = result_count.get(result_name) + 1;
		result_count.put(result_name, new_result_count);
		
		double new_total_cost = total_cost.get(result_name) + accumulated_cost;
		total_cost.put(result_name, new_total_cost);
	}
	
	public void add(String type_of_result, int[] link_utilization, double cost) {
		if (!matches_on_utilization_class.containsKey(type_of_result)){
			matches_on_utilization_class.put(type_of_result, new int[classes_cnt]);
			result_count.put(type_of_result, 0);
			total_cost.put(type_of_result, 0.0);
		}
		System.out.println("Adding " + cost + "to case: " + type_of_result);
		for (int lu : link_utilization)
			for (int c=1; c<this.classes_cnt; c++)
				if (lu < this.histogramm_class[c]){
					matches_on_utilization_class.get(type_of_result)[c-1]++;
					break;
				}
		
		int new_result_count = result_count.get(type_of_result) + 1;
		result_count.put(type_of_result, new_result_count);
		
		double new_total_cost = total_cost.get(type_of_result) + cost;
		total_cost.put(type_of_result, new_total_cost);
	}
	
	public void add_recovery_result(String result_name, int[] link_utilization, int hops) {
		// if this is the first result of this kind:
		if (!matches_on_utilization_class.containsKey(result_name)){
			matches_on_utilization_class.put(result_name, new int[classes_cnt]);
			result_count.put(result_name, 0);
			total_cost.put(result_name, 0.0);
			reconversion_hops.put(result_name, 0);
		}
		
		// adding links to the histogram
		for (int lu : link_utilization){
			int cl = 0;
			while (lu >= this.histogramm_class[cl])
				cl++;
			matches_on_utilization_class.get(result_name)[cl-1]++;
		}
		
		// calculating cost
		double accumulated_cost = 0.0;
		for (int lu : link_utilization)
			accumulated_cost += this.costFunc.calculate(0.001 * lu);
		
		int new_result_count = result_count.get(result_name) + 1;
		result_count.put(result_name, new_result_count);
		
		double new_total_cost = total_cost.get(result_name) + accumulated_cost;
		total_cost.put(result_name, new_total_cost);
		
		int total_hops = reconversion_hops.get(result_name) + hops;
		reconversion_hops.put(result_name, total_hops);
	}
	
	public void add4(String type_of_result, int[] link_utilization, double cost, int changed_LSAs) {
		if (!matches_on_utilization_class.containsKey(type_of_result)){
			matches_on_utilization_class.put(type_of_result, new int[classes_cnt]);
			result_count.put(type_of_result, 0);
			reconfigurations_in_OSPF.put(type_of_result, 0);
			total_cost.put(type_of_result, 0.0);
		}
		System.out.println("Adding " + cost + "to case: " + type_of_result);
		for (int lu : link_utilization)
			for (int c=1; c<this.classes_cnt; c++)
				if (lu < this.histogramm_class[c]){
					matches_on_utilization_class.get(type_of_result)[c-1]++;
					break;
				}
		
		int new_result_count = result_count.get(type_of_result) + 1;
		result_count.put(type_of_result, new_result_count);
		
		double new_total_cost = total_cost.get(type_of_result) + cost;
		total_cost.put(type_of_result, new_total_cost);
		
		int new_changedLSAs = reconfigurations_in_OSPF.get(type_of_result) + changed_LSAs;
		reconfigurations_in_OSPF.put(type_of_result, new_changedLSAs);
	}

	
	public void append_hops_to_converge(String name, int hops) {
		if (!reconversion_hops.keySet().contains(name))
			reconversion_hops.put(name, hops);
		else
			reconversion_hops.put(name, hops + reconversion_hops.get(name));
	}
	
	public void append_OSPF_reconfigurations(String name, int count){
		if (!reconfigurations_in_OSPF.keySet().contains(name))
			reconfigurations_in_OSPF.put(name, count);
		else
			reconfigurations_in_OSPF.put(name, count + reconfigurations_in_OSPF.get(name));
	}
	
}
