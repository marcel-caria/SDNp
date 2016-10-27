
public class TrafficFlow {
	
	private Double demand;
	private Router source;
	private Router destination;
	
	public TrafficFlow(Double demand, Router source, Router destination){
		if (source==null)
			throw new IllegalArgumentException("Source router can't be NULL");
		if (destination==null)
			throw new IllegalArgumentException("Destination router can't be NULL");
		this.demand = demand;
		this.source = source;
		this.destination = destination;
	}

	public Double getDemand() {
		return demand;
	}

	public Router getSource() {
		return source;
	}

	public Router getDestination() {
		return destination;
	}
	
	public void rescaleDemand(double scalingFactor){
		this.demand *= scalingFactor;
	}
	
}
