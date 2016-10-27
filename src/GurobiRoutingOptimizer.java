import java.util.HashMap;

import gurobi.*;

public class GurobiRoutingOptimizer {
	
	private Network network;
	private GRBEnv environment;
	private GRBModel model;
	
	public GurobiRoutingOptimizer(Network network, double maxGap, int timeLimit){
		this.network = network;
		try {
			environment = new GRBEnv("GUROBI.log");
			environment.set(GRB.DoubleParam.MIPGap, maxGap);
			if (timeLimit > 0)
				environment.set(GRB.DoubleParam.TimeLimit, timeLimit);
			model = new GRBModel(environment);
		}
		catch (GRBException e) {e.printStackTrace();}
	}
	
	
	public Result_load_balancing load_balance() throws GRBException {
		if ((network instanceof Network_SDN_Partitioned)){
			System.out.println("Starting GUROBI for load balancing in an SDN-partitioned network with "
					+ ((Network_SDN_Partitioned) network).get_number_of_bordernodes() + " border nodes.");
			this.addVariables_MetricVector();
			System.out.println("addVariables_MetricVector() done");
		}
		else if ((network instanceof Network_MPLS)){
			System.out.println("Starting GUROBI for load balancing in an MPLS network.");
		}
		else if ((network instanceof Network_Hybrid)){
			System.out.println("Starting GUROBI for load balancing in a hybrid SDN/OSPF network with "
					+ ((Network_Hybrid) network).get_number_of_hybrid_nodes() + " hybrid nodes.");
		}
		this.addVariables_Routing();
		System.out.println("addVariables_Routing() done");
		this.addVariables_Links();
		System.out.println("addVariables_Links() done");
		this.model.update();
		this.addConstraint_LinkLoad();
		System.out.println("addConstraint_LinkLoad() done");
		this.addConstraint_LinkUtilization();
		System.out.println("addConstraint_LinkUtilization() done");
		this.addConstraint_LinkUtilizationCost(LinearFunction.get_cost_function_for_load_balancing());
		System.out.println("addConstraint_LinkUtilizationCost() done");
		this.addConstraint_SinglePathRouting();
		System.out.println("addConstraint_SinglePathRouting() done");
		if ((network instanceof Network_SDN_Partitioned)){
			this.addConstraint_MetricVectorAdvertisement();
			System.out.println("addConstraint_MetricVectorAdvertisement() done");
			this.addConstraint_SingleMetricVector();
			System.out.println("addConstraint_SingleMetricVector() done");
		}
		this.model.optimize();
		if ((network instanceof Network_SDN_Partitioned))
			this.assign_optimized_metrics();
		this.print_loadbalancing_result();
		return getLoadBalancingResults();
	}
	
	public Result_load_balancing load_balance_after_linkfailure(double cost_for_each_new_LSA) throws GRBException {
		if ((network instanceof Network_SDN_Partitioned)){
			System.out.println("Starting GUROBI for load balancing after link failure in an SDN-partitioned network.");
			this.addVariables_MetricVector_after_linkfailure(cost_for_each_new_LSA);
			System.out.println("addVariables_MetricVector_after_linkfailure() done");
		}
		else{
			System.out.println("Starting GUROBI for load balancing in a hybrid SDN/OSPF or MPLS network.");
		}
		this.addVariables_Routing();
		System.out.println("addVariables_Routing() done");
		this.addVariables_Links();
		System.out.println("addVariables_Links() done");
		this.model.update();
		this.addConstraint_LinkLoad();
		System.out.println("addConstraint_LinkLoad() done");
		this.addConstraint_LinkUtilization();
		System.out.println("addConstraint_LinkUtilization() done");
		this.addConstraint_LinkUtilizationCost(LinearFunction.get_cost_function_for_load_balancing_after_link_failure());
		System.out.println("addConstraint_LinkUtilizationCost() done");
		this.addConstraint_SinglePathRouting();
		System.out.println("addConstraint_SinglePathRouting() done");
		if ((network instanceof Network_SDN_Partitioned)){
			this.addConstraint_MetricVectorAdvertisement();
			System.out.println("addConstraint_MetricVectorAdvertisement() done");
			this.addConstraint_SingleMetricVector();
			System.out.println("addConstraint_SingleMetricVector() done");
		}
		this.model.optimize();
//		this.print_loadbalancing_result();
//		this.print_new_advertisements();
		return getLoadBalancingResults();
	}

	public void minimize_capacities() throws GRBException {
		if ((network instanceof Network_SDN_Partitioned)){
			System.out.println("Starting GUROBI for link capacity minimization in an SDN-partitioned network with "
					+ ((Network_SDN_Partitioned) network).get_number_of_bordernodes() + " border nodes.");
			this.addVariables_MetricVector();
			System.out.println("addVariables_MetricVector() done");
		}
		else if ((network instanceof Network_MPLS)){
			System.out.println("Starting GUROBI for link capacity minimization in an MPLS network.");
		}
		else if ((network instanceof Network_Hybrid)){
			System.out.println("Starting GUROBI for link capacity minimization in a hybrid SDN/OSPF network with "
					+ ((Network_Hybrid) network).get_number_of_hybrid_nodes() + " hybrid nodes.");
		}
		this.addVariables_Routing();
		System.out.println("addVariables_Routing() done");
		this.addVariables_LinkCapacity();
		System.out.println("addVariables_LinkCapacity() done");
		this.addVariable_ObjectiveFunction();
		System.out.println("addVariable_ObjectiveFunction() done");
		this.model.update();
		this.addConstraint_ObjectiveFunction();
		System.out.println("addConstraint_ObjectiveFunction() done");
		this.addConstraint_LinkCapacities();
		System.out.println("addConstraint_LinkCapacities() done");
		this.addConstraint_SinglePathRouting();
		System.out.println("addConstraint_SinglePathRouting() done");
		if ((network instanceof Network_SDN_Partitioned)){
			this.addConstraint_MetricVectorAdvertisement();
			System.out.println("addConstraint_MetricVectorAdvertisement() done");
			this.addConstraint_SingleMetricVector();
			System.out.println("addConstraint_SingleMetricVector() done");
		}
		this.model.optimize();
	}
	
	
	private void addVariables_Routing() throws GRBException{
		for (Router source : network.getRouters()){
			for (Router destination : network.getRouters()){
				if (source.equals(destination))
					continue;
				MultiPath multi = null;
				if ((network instanceof Network_SDN_Partitioned))
					multi = ((Router_OSPF) source).getMultiMetaPath(destination);
				if (this.network instanceof Network_MPLS)
					multi = ((Router_MPLS) source).getMultiPath(destination);
				if (this.network instanceof Network_Hybrid)
					multi = ((Router_OSPF) source).getMultiPath(destination);
				for (Path path : multi.getPaths()){
					GRBVar newGurobiVariable = this.model.addVar(0, 1, 0, GRB.BINARY, null);
					path.assign_GUROBI_Variable(newGurobiVariable);
				}
			}
		}
	}
	
	private void addVariables_MetricVector() throws GRBException{
		for (Network_OSPF subdomain : ((Network_SDN_Partitioned) network).getSubdomains())
			for (Router external : this.network.getRouters())
				if (!subdomain.getRouters().contains(external))
					for (MetricVector vector : subdomain.getMetricVectors()){
						GRBVar newGurobiVariable = this.model.addVar(0, 1, 0, GRB.BINARY, null);
						vector.assign_GUROBI_Variable(external, newGurobiVariable);
					}
	}
	
	private void addVariables_MetricVector_after_linkfailure(double cost_for_each_new_LSA) throws GRBException{
		for (Network_OSPF subdomain : ((Network_SDN_Partitioned) network).getSubdomains())
			for (Router external : this.network.getRouters())
				if (!subdomain.getRouters().contains(external))
					for (MetricVector vector : subdomain.getMetricVectors()){
						GRBVar newGurobiVariable = this.model.addVar(0, 1, cost_for_each_new_LSA * subdomain.number_of_new_advertisements(vector, external), GRB.BINARY, null);
						vector.assign_GUROBI_Variable(external, newGurobiVariable);
					}
	}
	
	private void addVariables_LinkCapacity() throws GRBException{
		for (Link link : network.getLinks())
			for (Integer cap : network.getAvailableLinkCapacityTypes()){
				GRBVar newGurobiVariable = this.model.addVar(0, 1, 0, GRB.BINARY, null);
				link.set_GUROBI_Variable(newGurobiVariable, cap.toString());
			}
	}
	
	private void addVariables_Links() throws GRBException{
		for (Link link : network.getLinks()){
			GRBVar linkloadVariable = this.model.addVar(0, 50000, 0, GRB.CONTINUOUS, null);
			link.set_GUROBI_Variable(linkloadVariable, "linkLoad");
			GRBVar utilizationVariable = this.model.addVar(0, 50, 0, GRB.CONTINUOUS, null);
			link.set_GUROBI_Variable(utilizationVariable, "linkUtilization");
			GRBVar costVariable = this.model.addVar(0, 5000000, 1, GRB.CONTINUOUS, null);
			link.set_GUROBI_Variable(costVariable, "utilizationCost");
		}
	}
	
	private void addVariable_ObjectiveFunction() throws GRBException{
		GRBVar objectiveFunction = model.addVar(0, 1000000000, 1, GRB.INTEGER, null);
		network.assign_GUROBI_Variable(objectiveFunction);
	}
	
	private void addConstraint_ObjectiveFunction() throws GRBException{
		GRBLinExpr totalLinkCap = new GRBLinExpr();
		for (Link link : network.getLinks())
			for (Integer capacity : network.getAvailableLinkCapacityTypes())
				totalLinkCap.addTerm(capacity, link.get_GUROBI_Variable(capacity.toString()));
		this.model.addConstr(totalLinkCap, GRB.EQUAL, network.get_GUROBI_Variable(), null);
	}
	
	private void addConstraint_LinkLoad() throws GRBException{
		HashMap<Link, GRBLinExpr> linkloads = new HashMap<Link, GRBLinExpr>();
		for (Link link : this.network.getLinks())
			linkloads.put(link, new GRBLinExpr());
		for (TrafficFlow flow : this.network.getTraffic()){
			Router source = flow.getSource();
			Router destination = flow.getDestination();
			MultiPath multi = null;
			if ((network instanceof Network_SDN_Partitioned))
				multi = ((Router_OSPF) source).getMultiMetaPath(destination);
			if (this.network instanceof Network_MPLS)
				multi = ((Router_MPLS) source).getMultiPath(destination);
			if (this.network instanceof Network_Hybrid)
				multi = ((Router_OSPF) source).getMultiPath(destination);
			for (Path path : multi.getPaths())
				for (Link link : path.getLinks())
					linkloads.get(link).addTerm(flow.getDemand(), path.get_GUROBI_Variable());
		}
		for (Link link : this.network.getLinks()){
			model.addConstr(link.get_GUROBI_Variable("linkLoad"), GRB.EQUAL, linkloads.get(link), null);
		}
	}
	
	private void addConstraint_LinkUtilization() throws GRBException{
		for (Link link : this.network.getLinks()){
			GRBLinExpr utilization = new GRBLinExpr();
			utilization.addTerm(1.0/link.getBandwidth(), link.get_GUROBI_Variable("linkLoad"));
			model.addConstr(link.get_GUROBI_Variable("linkUtilization"), GRB.EQUAL, utilization, null);
		}
	}
	
	private void addConstraint_LinkUtilizationCost(LinearFunction cost) throws GRBException{
		for (Link link : this.network.getLinks())
			for (LinearFunction func : cost.functions){
				GRBLinExpr lowerCostBound = new GRBLinExpr();
				lowerCostBound.addTerm(func.a, link.get_GUROBI_Variable("linkUtilization"));
				lowerCostBound.addConstant(func.b);
				model.addConstr(link.get_GUROBI_Variable("utilizationCost"), GRB.GREATER_EQUAL, lowerCostBound, null);
			}
	}
	
	private void addConstraint_LinkCapacities() throws GRBException{
		HashMap<Link, GRBLinExpr> linkloads = new HashMap<Link, GRBLinExpr>();
		for (Link link : this.network.getLinks())
			linkloads.put(link, new GRBLinExpr());
		for (TrafficFlow flow : this.network.getTraffic()){
			Router source = flow.getSource();
			Router destination = flow.getDestination();
			MultiPath multi = null;
			if ((network instanceof Network_SDN_Partitioned))
				multi = ((Router_OSPF) source).getMultiMetaPath(destination);
			if (this.network instanceof Network_MPLS)
				multi = ((Router_MPLS) source).getMultiPath(destination);
			if (this.network instanceof Network_Hybrid)
				multi = ((Router_OSPF) source).getMultiPath(destination);
			for (Path path : multi.getPaths())
				for (Link link : path.getLinks())
					linkloads.get(link).addTerm(flow.getDemand(), path.get_GUROBI_Variable());
		}
		for (Link link : this.network.getLinks()){
			GRBLinExpr effective_capacity = new GRBLinExpr();
			for (Integer capacity : this.network.getAvailableLinkCapacityTypes())
				effective_capacity.addTerm(capacity, link.get_GUROBI_Variable(capacity.toString()));
			model.addConstr(effective_capacity, GRB.GREATER_EQUAL, linkloads.get(link), null);
			GRBLinExpr single_type_used = new GRBLinExpr();
			for (Integer capacity : this.network.getAvailableLinkCapacityTypes())
				single_type_used.addTerm(1, link.get_GUROBI_Variable(capacity.toString()));
			model.addConstr(single_type_used, GRB.EQUAL, 1, null);
		}
	}
	
	private void addConstraint_SinglePathRouting() throws GRBException{
		for (Router source : this.network.getRouters())
			for (Router destination : this.network.getRouters())
				if (!source.equals(destination)){
					GRBLinExpr singlePathExpr = new GRBLinExpr();
					MultiPath multi = null;
					if ((network instanceof Network_SDN_Partitioned))
						multi = ((Router_OSPF) source).getMultiMetaPath(destination);
					if (this.network instanceof Network_MPLS)
						multi = ((Router_MPLS) source).getMultiPath(destination);
					if (this.network instanceof Network_Hybrid)
						multi = ((Router_OSPF) source).getMultiPath(destination);
					for (Path path : multi.getPaths())
						singlePathExpr.addTerm(1, path.get_GUROBI_Variable());
					model.addConstr(singlePathExpr, GRB.EQUAL, 1, null);
				}
	}
	
	private void addConstraint_MetricVectorAdvertisement() throws GRBException{
		for (Router source : this.network.getRouters())
			for (Router destination : this.network.getRouters())
				if (!source.equals(destination)){
					MultiMetaPath multi = ((Router_OSPF) source).getMultiMetaPath(destination);
					for (Path path : multi.getPaths()){
						MetaPath meta = (MetaPath) path;
						GRBLinExpr advertisementExpr = new GRBLinExpr();
						for (Network_OSPF subdomain : ((Network_SDN_Partitioned) this.network).getSubdomains())
							if (meta.requires_MetricVector_in(subdomain)){
								for (MetricVector vector : subdomain.getMetricVectors())
									if (vector.allows(meta))
										advertisementExpr.addTerm(1, vector.get_GUROBI_Variable(destination));
								model.addConstr(advertisementExpr, GRB.GREATER_EQUAL, meta.get_GUROBI_Variable(), null);
							}
					}
				}
	}
	
	private void addConstraint_SingleMetricVector() throws GRBException{
		for (Network_OSPF subdomain : ((Network_SDN_Partitioned) this.network).getSubdomains())
			for (Router destination : this.network.getRouters())
				if (!subdomain.getRouters().contains(destination)){
					GRBLinExpr advertisementExpr = new GRBLinExpr();
					for (MetricVector vector : subdomain.getMetricVectors())
						advertisementExpr.addTerm(1, vector.get_GUROBI_Variable(destination));
					model.addConstr(advertisementExpr, GRB.EQUAL, 1, null);
				}
	}
	
	// to store LSAs before a link failure occurs
	private void assign_optimized_metrics() throws GRBException{
		for (Network_OSPF subdomain : ((Network_SDN_Partitioned) network).getSubdomains())
			for (Router external : this.network.getRouters())
				if (!subdomain.getRouters().contains(external))
					for (MetricVector vector : subdomain.getMetricVectors())
						if (vector.get_GUROBI_Variable(external).get(GRB.DoubleAttr.X) > 0.5)
							for (Router_SDN_Border bordernode : ((Network_SDN_Partitioned) network).get_bordernodes())
								if (vector.contains_metric_for(bordernode))
									bordernode.setMetric(subdomain, external, vector);
	}
	
	private Result_load_balancing getLoadBalancingResults() throws GRBException {
		Result_load_balancing resultObj = new Result_load_balancing();
		for (Link link : network.getLinks()){
			Integer bandwidth = link.getBandwidth();
			Double load = link.get_GUROBI_Variable("linkLoad").get(GRB.DoubleAttr.X);
			resultObj.add(load, bandwidth);
		}
		int metricReconfigurations = 0;
		if (this.network instanceof Network_SDN_Partitioned){
			Network_SDN_Partitioned partitioned = (Network_SDN_Partitioned) this.network;
			for (Network_OSPF subdomain : partitioned.getSubdomains()){
				for (Router external : partitioned.getRouters()){
					if (!subdomain.getRouters().contains(external))
						for (MetricVector vector : subdomain.getMetricVectors())
							if (vector.get_GUROBI_Variable(external).get(GRB.DoubleAttr.X) > 0.5)
								metricReconfigurations += subdomain.getRegularNodes().size() *
										subdomain.number_of_new_advertisements(vector, external);
				}
			}
		}
		resultObj.metricReconfigurations += metricReconfigurations;
		return resultObj;
	}
	
	private void print_loadbalancing_result() throws GRBException{
		System.out.println("\r\nLink utilizations after routing optimization:");
		double totalCost = 0.0;
		double excessDemand = 0.0;
		for (Link link : network.getLinks()){
			if (Math.round(link.get_GUROBI_Variable("linkLoad").get(GRB.DoubleAttr.X)) > link.getBandwidth())
				excessDemand += Math.round(link.get_GUROBI_Variable("linkLoad").get(GRB.DoubleAttr.X)) - link.getBandwidth();
			if (link.get_GUROBI_Variable("utilizationCost").get(GRB.DoubleAttr.X) < 0.000001)
				continue;
			System.out.println(link.toString() + ": " + link.getBandwidth() + " Gbit/s, " 
					+ Math.round(link.get_GUROBI_Variable("linkLoad").get(GRB.DoubleAttr.X)) + " Gbit/s ("
					+ Math.round(link.get_GUROBI_Variable("linkUtilization").get(GRB.DoubleAttr.X)*100) + "%) = "
					+ Math.round(link.get_GUROBI_Variable("utilizationCost").get(GRB.DoubleAttr.X)*1000) + " m$");
			totalCost += link.get_GUROBI_Variable("utilizationCost").get(GRB.DoubleAttr.X);
		}
		System.out.println("Total Link Utilization Cost: " + Math.round(totalCost));
		int ppm = (int) Math.round(excessDemand / network.getTotalTrafficDemand() * 1000.0);
		System.out.println("Excess traffic demand: " + Math.round(excessDemand) + "  (" + ppm + "ppm)");
	}
	
	public void print_new_advertisements() throws GRBException{
		int total = 0;
		System.out.println();
		for (Network_OSPF subdomain : ((Network_SDN_Partitioned) network).getSubdomains()){
//			System.out.println("New metrics advertised in sub-domain number " + ((Network_SDN_Partitioned) network).getSubdomains().indexOf(subdomain) + ":");
			for (Router external : this.network.getRouters()){
				int cnt = 0;
				if (!subdomain.getRouters().contains(external))
					for (MetricVector vector : subdomain.getMetricVectors())
						if (vector.get_GUROBI_Variable(external).get(GRB.DoubleAttr.X) > 0.5)
							cnt += subdomain.number_of_new_advertisements(vector, external);
				if (cnt<1)
					continue;
//				System.out.println("To " + external.getName() + ": " + cnt);
				total += cnt;
			}
		}
		System.out.println("Total number of new metrics: " + total);
	}
	
	
}