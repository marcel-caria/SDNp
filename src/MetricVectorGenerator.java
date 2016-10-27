import java.util.HashMap;
import java.util.LinkedList;

public class MetricVectorGenerator {
	
	private Network_OSPF subdomain;
	private LinkedList<Router_OSPF> regularNodes_to_consider;
	private LinkedList<Router_SDN_Border> borderNodes;
	private LinkedList<MetricVector> metricVectors;
	
	public MetricVectorGenerator(Network_OSPF subdomain, int maxLSAs) {
		this.subdomain = subdomain;
		this.regularNodes_to_consider = new LinkedList<Router_OSPF>(subdomain.getRegularNodes());
		this.borderNodes = subdomain.getBorderNodes();
		this.removeInsignificantNodes();
		this.metricVectors = new LinkedList<MetricVector>();
		this.retrieveOptimumMetricVectors();
		this.generateMetricVectors();
	}
	
	
	/**
	 * This function 
	 */
	private void retrieveOptimumMetricVectors() {
		Router_SDN_Border bordernode = this.borderNodes.getFirst();
		if (!bordernode.has_old_metrics())
			return;
		Router neighbor = null;
		for (Router n : bordernode.getNeighbors())
			if (this.subdomain.getRegularNodes().contains(n))
				neighbor = n;
		this.recycle(bordernode.get_old_metrics_advertised_to(neighbor));
	}

	private void recycle(LinkedList<MetricVector> old_metrics) {
		System.out.println("Recycling " + old_metrics.size() + " old metrics.");
		for (MetricVector oldVector : old_metrics)
			this.metricVectors.add(oldVector.convert_to(this.subdomain));
	}


	void generateMetricVectors(){
		HashMap<Router_SDN_Border, Long> weights = new HashMap<Router_SDN_Border, Long>();
		long wgt = 0L;
		for (Router_SDN_Border borNode : this.borderNodes){
			weights.put(borNode, wgt);
			wgt += 10000000;
		}
		MetricVector metricVector = new MetricVector(this.subdomain, weights);
		if (providesNewExitMatrix(metricVector))
			this.add(metricVector);
		boolean finished = false;
		int cnt = 0;
		while (!finished){
			cnt++;
			HashMap<Router_SDN_Border, Long> nextWeights = getNextWeights(metricVector);
			finished = (nextWeights == null);
			if (finished)
				break;
			metricVector = new MetricVector(this.subdomain, nextWeights);
			if (providesNewExitMatrix(metricVector))
				this.add(metricVector);
		}
		System.out.println("Found " + this.metricVectors.size() + " MetricVectors with " + cnt + " iterations.");
	}
	
	private void add(MetricVector metricVector) {
		this.metricVectors.add(metricVector);
	}

	private boolean providesNewExitMatrix(MetricVector nextVector) {
		for (MetricVector mv : this.metricVectors)
			if (mv.sameRouting(nextVector))
				return false;
		return true;
	}

	public HashMap<Router_SDN_Border, Long> getNextWeights(MetricVector mv) {
		HashMap<Router_SDN_Border, Long> newWeights = new HashMap<Router_SDN_Border, Long>();
		Router_SDN_Border bn_to_be_updated = this.firstWhoIsExit(mv);
		// check end condition
		if (bn_to_be_updated.equals(this.subdomain.getBorderNodes().getLast()))
			return null;
		for (int i=0; i<this.subdomain.getBorderNodes().indexOf(bn_to_be_updated); i++)
			newWeights.put(this.subdomain.getBorderNodes().get(i), 0L);
		newWeights.put(bn_to_be_updated, increaseMetric(bn_to_be_updated, mv));
		for (int i=this.subdomain.getBorderNodes().indexOf(bn_to_be_updated)+1; i<this.subdomain.getBorderNodes().size(); i++)
			newWeights.put(this.subdomain.getBorderNodes().get(i), mv.getWeight(this.subdomain.getBorderNodes().get(i)));
		return newWeights;
	}

	private Router_SDN_Border firstWhoIsExit(MetricVector mv){
		for (Router_SDN_Border bn : this.subdomain.getBorderNodes())
			if (!mv.nodes_that_forward_to(bn).isEmpty())
				return bn;
		return null;
	}
	
	private Long increaseMetric(Router_SDN_Border borderNode, MetricVector mv) {
		// find the two smallest metric increases at which some regular node switches its border node
		LinkedList<Long> two_smallest_increases = new LinkedList<Long>();
		for (Router_OSPF testThisReg : mv.nodes_that_forward_to(borderNode)){
			Long dist = distance_via(testThisReg, borderNode, mv);
			Long increase = 100000000000L;
			for (int i=this.subdomain.getBorderNodes().indexOf(borderNode)+1; i<this.subdomain.getBorderNodes().size(); i++){
				Router_SDN_Border other_bn = this.subdomain.getBorderNodes().get(i);
				if (other_bn.equals(borderNode))
					continue;
				Long newIncrease = distance_via(testThisReg, other_bn, mv) - dist;
				if (increase > newIncrease)
					increase = newIncrease;
			}
			int index = 0;
			for (Long otherIncrease : two_smallest_increases)
				if (increase > otherIncrease)
					index++;
			two_smallest_increases.add(index, increase);
			if (two_smallest_increases.size() > 2)
				two_smallest_increases.removeLast();
		}
		if (two_smallest_increases.isEmpty())
			throw new Error("ERROR: two_smallest_increases is empty!");
		if (two_smallest_increases.size()==1 || two_smallest_increases.getLast()-two_smallest_increases.getFirst()<2)
			return mv.getWeight(borderNode) + two_smallest_increases.getLast() + 1;
		return mv.getWeight(borderNode) + (int) Math.round(0.5*two_smallest_increases.getFirst() + 0.5*two_smallest_increases.getLast());
	}
	
	private Long distance_via(Router_OSPF regularNode, Router_SDN_Border borderNode, MetricVector mv){
		return regularNode.getPath(borderNode).getPathMetric() + mv.getWeight(borderNode);
	}
	
	// to reduce complexity
	public void reduce_size(int upperbound, long seed){
//		boolean use_only_basic_LSAs = false;
//		if (upperbound == -1){
//			use_only_basic_LSAs = true;
//			upperbound = 2;
//		}
//		boolean use_some_more_LSAs = false;
//		if (upperbound == -2){
//			use_some_more_LSAs = true;
//			upperbound = 2;
//		}
//		LinkedList<MetricVector> removeList = new LinkedList<MetricVector>();
//		for (MetricVector sol : removeList)
//			metricVectors.remove(sol);
//		if (metricVectors.size() <= upperbound)
//			return;
//		LinkedList<MetricVector> newList = new LinkedList<MetricVector>();
//		
//		// add the solution with all zero weights
//		int ospf_node_cnt = regularNodes.size() + ignored_OSPF_nodes.keySet().size();
//		for (MetricVector sol : metricVectors){
//			boolean skipthisone = false;
//			for (Router_SDN_Border bn : this.borderNodes)
//				if (sol.getWeight(bn) > 0)
//					skipthisone = true;
//			if (skipthisone)
//				continue;
//			newList.add(sol);
//			break;
//		}
//		// and solutions with vectors like (0,0,12,0,0,0)
//		for (MetricVector sol : metricVectors){
//			boolean skip_this_solution = false;
//			for (int b=0; b<sol.exitVector().length; b++)
//				if (sol.exitVector()[b] > 0 && sol.exitVector()[b] < ospf_node_cnt){
//					skip_this_solution = true;
//					break;
//				}
//			if (skip_this_solution)
//				continue;
//			newList.add(sol);
//		}
//		if (use_only_basic_LSAs){
//			metricVectors = newList;
//			return;
//		}
//		if (use_some_more_LSAs)
//			upperbound = newList.size() * 2;
//		for (MetricVector sol : newList)
//			metricVectors.remove(sol);
//		if (metricVectors.isEmpty())
//			return;
//		
//		// add the most balanced solution, i.e., with a vector that looks somehow like (1,2,1,1,2,2)
//		int smallest_val = 10000;
//		MetricVector best_solution = metricVectors.getFirst();
//		for (MetricVector sol : metricVectors){
//			int val = 0;
//			for (int b=0; b<borderNodes.size(); b++)
//				val += sol.exitVector()[b] * sol.exitVector()[b];
//			if (smallest_val > val){
//				smallest_val = val;
//				best_solution = sol;
//			}
//		}
//				
//		newList.add(best_solution);
//		metricVectors.remove(best_solution);
//		
//		// iteratively add solutions that have exit vectors most different to already added solutions
//		Random rand = new Random(seed);
//		while (newList.size() < upperbound && !metricVectors.isEmpty()){
//			
//			LinkedList<MetricVector> candidateNodes = new LinkedList<MetricVector>();
//			int largest_min_diff = 0;
//			for (MetricVector some_sol : metricVectors){
//				int min_diff = 10000;
//				for (MetricVector good_sol : newList){
//					int diff = 0;
//					for (int b=0; b<borderNodes.size(); b++)
//						diff += Math.abs(some_sol.exitVector()[b] - good_sol.exitVector()[b]);
//					if (min_diff > diff)
//						min_diff = diff;
//				}
//				if (largest_min_diff > min_diff)
//					continue;
//				if (largest_min_diff == min_diff){
//					candidateNodes.add(some_sol);
//					continue;
//				}
//				largest_min_diff = min_diff;
//				candidateNodes = new LinkedList<MetricVector>();
//				candidateNodes.add(some_sol);
//			}
//			MetricVector drawn_solution = candidateNodes.get(rand.nextInt(candidateNodes.size()));
//			newList.add(drawn_solution);
//			metricVectors.remove(drawn_solution);
//		}
//		metricVectors = newList;
	}
	
	private void removeInsignificantNodes(){
		LinkedList<Router_OSPF> deleteList = new LinkedList<Router_OSPF>();
		for (Router_OSPF router_1 : this.regularNodes_to_consider){
			if (deleteList.contains(router_1))
				continue;
			for (Router_OSPF router_2 : this.regularNodes_to_consider){
				if (deleteList.contains(router_1) || deleteList.contains(router_2) || router_1.equals(router_2))
					continue;
				int diff = router_1.getPath(this.borderNodes.getFirst()).getPathMetric()
						- router_2.getPath(this.borderNodes.getFirst()).getPathMetric();
				boolean one_is_insignificant = true;
				for (Router_SDN_Border borderNode : this.borderNodes)
					if (router_1.getPath(borderNode).getPathMetric() - router_2.getPath(borderNode).getPathMetric() != diff){
						one_is_insignificant = false;
						break;
					}
				if (one_is_insignificant){
					if (diff > 0)
						deleteList.add(router_1);
					else
						deleteList.add(router_2);
				}
			}
		}
		for (Router_OSPF delete : deleteList)
			this.regularNodes_to_consider.remove(delete);
		System.out.println(deleteList.size() + " OSPF nodes are ignored for link weight generation.");
	}
	
	public LinkedList<MetricVector> getMetricVectors() {
		return this.metricVectors;
	}
	
}
