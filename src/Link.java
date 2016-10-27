import java.util.HashMap;

import gurobi.GRBVar;

public class Link {
	
	private Router source;
	private Router destination;
	private int bandwidth;
	private int weight;
	private double trafficLoad;
	private boolean active;
	private HashMap<String, GRBVar> gurobiVariables;
//	private GRBVar linkLoad;
//	private GRBVar linkUtilization;
//	private GRBVar utilizationCost;
	
	
	public Link (Router source, Router destination){
		if (source==null)
			throw new IllegalArgumentException("Source router can't be NULL");
		if (destination==null)
			throw new IllegalArgumentException("Destination router can't be NULL");
		this.source = source;
		this.destination = destination;
		this.trafficLoad = 0.0;
		this.active = true;
		this.gurobiVariables = new HashMap<String, GRBVar>();
	}
	
	
	public GRBVar get_GUROBI_Variable(String varName) {
		return this.gurobiVariables.get(varName);
	}
	
	public void set_GUROBI_Variable(GRBVar variable, String varName) {
		this.gurobiVariables.put(varName, variable);
	}
	
	public int getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public double getTrafficLoad() {
		return trafficLoad;
	}

	public void setTrafficLoad(double trafficLoad) {
		this.trafficLoad = trafficLoad;
	}

	public void addTrafficLoad(double trafficLoad) {
		this.trafficLoad += trafficLoad;
	}

	public Router getSource() {
		return source;
	}

	public Router getDestination() {
		if (!active)
			throw new Error("ERROR: link from " + this.source.getName() + " to " + this.destination.getName() 
					+ " is disabled and can not be used!");;
		return destination;
	}

	public void disable() {
		if (!this.active){
			System.out.println("WARNING! Disabling disabled link " + toString());
			return;
		}
		this.active = false;
	}
	
	public void enable() {
		if (this.active){
			System.out.println("WARNING! Enabling enabled link " + toString());
			return;
		}
		this.active = true;
	}
	
	public boolean isEnabled(){
		return this.active;
	}
	
	@Override
	public String toString(){
		return "(" + this.source.getName() + ", " + this.destination.getName() + ")";
	}
	
	public String monitoringInfo(){
		return "Link " + this.toString() + ": "
				+ this.bandwidth + " Gbit/s capacity, "
				+ (int) Math.round(this.trafficLoad) + " Gbit/s traffic, "
				+ (int) Math.round(this.trafficLoad/(0.01*this.bandwidth)) + "% utilization.";
	}
	
	public String monitoringInfo(LinearFunction cost){
		return "Link " + this.toString() + ": "
				+ this.bandwidth + " Gbit/s capacity, "
				+ (int) Math.round(this.trafficLoad) + " Gbit/s traffic, "
				+ (int) Math.round(this.trafficLoad/(0.01*this.bandwidth)) + "% utilization, "
				+ (int) Math.round(this.get_OSPF_utilization_cost(cost) * 1000.0) + " millis cost.";
	}
	
	public boolean sameLink(Link other){
	    if (other == null) return false;
	    if (other == this) return true;
	    return (other.getSource().sameNode(this.source) && other.getDestination().sameNode(this.destination));
	}
	
	public boolean reverseLink(Link other){
	    if (other == null) return false;
	    if (other == this) return false;
	    return (other.getSource().sameNode(this.destination) && other.getDestination().sameNode(this.source));
	}
	
	public double get_OSPF_utilization_cost(LinearFunction cost){
		double util = 1.0 * this.trafficLoad / this.bandwidth;
		double utilCost = 0.0;
		for (LinearFunction func : cost.functions){
			double lowerBound = func.a * util + func.b;
			if (utilCost < lowerBound)
				utilCost = lowerBound;
		}
		return utilCost;
	}
	
}
