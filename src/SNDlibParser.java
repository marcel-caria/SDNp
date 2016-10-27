import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class SNDlibParser {
	
	private String fileName;
	private LinkedList<String> lines;
	private HashMap<String, Integer> linkWeights;
	private LinkedList<String> nodenames;
	private LinkedList<LinkedList<String>> bordernodeLists;
	private LinkedList<LinkedList<String>> hybridNodeLists;
	private static final Long randomSeed = 5098345098345098345L;
	
	public SNDlibParser(String fileName){
		this.fileName = fileName;
		this.readFile();
		this.parseNodeSection();
		this.parseLinkSection();
		this.parseBordernodeSection();
		this.parseHybridNodeSection();
	}
	
	private void readFile(){
        BufferedReader text;
        String nextLine;
        this.lines = new LinkedList<String>();
        try{
            text = new BufferedReader(new FileReader(fileName));
            while ((nextLine = text.readLine()) != null){
            	nextLine = nextLine.trim();
            	// discard comments
        		if (nextLine.startsWith("#"))
        			continue;
            	// discard empty lines
        		if (nextLine.length() == 0)
        			continue;
        		this.lines.add(nextLine);
            }
            text.close();
        }
        catch (IOException e){
            System.out.println("\r\nERROR reading file " + fileName + ": no such file or read error.");
        }
	}
	
	private void parseNodeSection(){
        // find NODES section
		int lineNumber = 0;
        while (!this.lines.get(lineNumber).startsWith("NODES")){
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("No NODES section found in SNDlib file " + this.fileName);
        }
    	lineNumber++;
        // read all lines with node definitions
		this.nodenames = new LinkedList<String>();
        while (!this.lines.get(lineNumber).equals(")")){
        	this.nodenames.add(this.lines.get(lineNumber).split(" ")[0]);
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("NODES section not closed in SNDlib file " + this.fileName);
        }
	}
	
	private void parseLinkSection(){
        // find LINKS section
		int lineNumber = 0;
        while (!this.lines.get(lineNumber).startsWith("LINKS")){
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("No LINKS section found in SNDlib file " + this.fileName);
        }
    	lineNumber++;
        // read all lines with link definitions
        this.linkWeights = new HashMap<String, Integer>();
        while (!this.lines.get(lineNumber).equals(")")){
    		String betweenBrackets = this.lines.get(lineNumber).substring(
    				this.lines.get(lineNumber).indexOf("(")+1, this.lines.get(lineNumber).indexOf(")")).trim();
    		String node_a = betweenBrackets.split(" ")[0];
    		String node_b = betweenBrackets.split(" ")[1];
    		int i = this.nodenames.indexOf(node_a);
    		int j = this.nodenames.indexOf(node_b);
    		if (i<0 || j<0){
    			System.out.println("No such link: " + node_a + " - " + node_b);
    			continue;
    		}
    		this.linkWeights.put(node_a + "-" + node_b, 30000);
    		this.linkWeights.put(node_b + "-" + node_a, 30000);
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("LINKS section not closed in SNDlib file " + this.fileName);
        }
	}
	
	public void randomize_OSPF_link_weights(){
		Random random = new Random(SNDlibParser.randomSeed);
		for (String key : this.linkWeights.keySet())
			this.linkWeights.put(key, this.linkWeights.get(key) - 500 + random.nextInt(1000));
	}
	
	private void parseBordernodeSection(){
        // find BORDERNODES section
		int lineNumber = 0;
        while (!this.lines.get(lineNumber).startsWith("BORDERNODES")){
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("No BORDERNODES section found in SNDlib file " + this.fileName);
        }
    	lineNumber++;
        // read all lines with border nodes
        this.bordernodeLists = new LinkedList<LinkedList<String>>();
        while (!this.lines.get(lineNumber).equals(")")){
        	LinkedList<String> nextList = new LinkedList<String>();
        	this.bordernodeLists.add(nextList);
    		String betweenBrackets = this.lines.get(lineNumber).substring(
    				this.lines.get(lineNumber).indexOf("(")+1, this.lines.get(lineNumber).indexOf(")")).trim();
    		String[] nodes = betweenBrackets.split(" ");
    		for (String nextnode : nodes)
    			nextList.add(nextnode);
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("BORDERNODES section not closed in SNDlib file " + this.fileName);
        }
	}

	private void parseHybridNodeSection(){
        // find HYBRIDNODES section
		int lineNumber = 0;
        while (!this.lines.get(lineNumber).startsWith("HYBRIDNODES")){
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("No HYBRIDNODES section found in SNDlib file " + this.fileName);
        }
    	lineNumber++;
        // read all lines with hybrid nodes
        this.hybridNodeLists = new LinkedList<LinkedList<String>>();
        while (!this.lines.get(lineNumber).equals(")")){
        	LinkedList<String> nextList = new LinkedList<String>();
        	this.hybridNodeLists.add(nextList);
    		String betweenBrackets = this.lines.get(lineNumber).substring(
    				this.lines.get(lineNumber).indexOf("(")+1, this.lines.get(lineNumber).indexOf(")")).trim();
    		String[] nodes = betweenBrackets.split(" ");
    		for (String nextnode : nodes)
    			nextList.add(nextnode);
        	lineNumber++;
        	if (this.lines.size() <= lineNumber)
        		throw new Error("HYBRIDNODES section not closed in SNDlib file " + this.fileName);
        }
	}
	
	void buildBidirectionalLinks(LinkedList<Router> nodes, String failedLink){
		for (Router a : nodes)
			for (Router b : nodes){
				String key_1 = a.getName() + "-" + b.getName();
				String key_2 = b.getName() + "-" + a.getName();
				if (!this.linkWeights.containsKey(key_1) || !this.linkWeights.containsKey(key_2))
					continue;
				if (failedLink != null)
					if (key_1.equals(failedLink) || key_2.equals(failedLink))
						continue;
				Link link_1 = new Link(a, b);
				link_1.setWeight(this.linkWeights.get(key_1));
				Link link_2 = new Link(b, a);
				link_2.setWeight(this.linkWeights.get(key_2));
				a.addBidirectionalLink(link_1, link_2);
				b.addBidirectionalLink(link_1, link_2);
			}
	}
	
	public Network_OSPF build_OSPF_Network(String failedLink){
		if (failedLink != null)
			System.out.println("OSPF network: Simulating Link Failure on Link " + failedLink);
		LinkedList<Router> theNodes = new LinkedList<Router>();
		for (String node : this.nodenames)
			theNodes.add(new Router_OSPF(this.nodenames.indexOf(node), node));
		this.buildBidirectionalLinks(theNodes, failedLink);
		Network_OSPF network = new Network_OSPF(theNodes, "OSPF");
		if (failedLink != null)
			network.set_OSPF_routing_reconfigurations_due_to_a_link_failure(theNodes.size() * 2);
		else
			network.set_OSPF_routing_reconfigurations_due_to_a_link_failure(0);
		return network;
	}
	
	public Network_MPLS build_MPLS_Network(String failedLink){
		if (failedLink != null)
			System.out.println("MPLS network: Simulating Link Failure on Link " + failedLink);
		LinkedList<Router> theNodes = new LinkedList<Router>();
		for (String node : this.nodenames)
			theNodes.add(new Router_MPLS(this.nodenames.indexOf(node), node));
		this.buildBidirectionalLinks(theNodes, failedLink);
		Network_MPLS network = new Network_MPLS(theNodes, "MPLS");
		network.set_OSPF_routing_reconfigurations_due_to_a_link_failure(0);
		return network;
	}
	
	public Network_Hybrid build_Hybrid_Network(Integer index, String failedLink){
		if (failedLink != null)
			System.out.println("Hybrid network number + " + index + ": Simulating Link Failure on Link " + failedLink);
		LinkedList<Router> theNodes = new LinkedList<Router>();
		for (String node : this.nodenames)
			if (hybridNodeLists.get(index).contains(node))
				theNodes.add(new Router_OSPF_SDNenabled(this.nodenames.indexOf(node), node));
			else
				theNodes.add(new Router_OSPF(this.nodenames.indexOf(node), node));
		this.buildBidirectionalLinks(theNodes, failedLink);
		Network_Hybrid network = new Network_Hybrid(theNodes, "Hybrid-" + index);
		if (failedLink != null)
			network.set_OSPF_routing_reconfigurations_due_to_a_link_failure(theNodes.size() * 2);
		else
			network.set_OSPF_routing_reconfigurations_due_to_a_link_failure(0);
		return network;
	}
	
	public LinkedList<Network_Hybrid> build_Hybrid_Networks(String failedLink){
		LinkedList<Network_Hybrid> networks = new LinkedList<Network_Hybrid>();
		for (int index=0; index<this.hybridNodeLists.size(); index++)
			networks.add(this.build_Hybrid_Network(index, failedLink));
		return networks;
	}
	
	public Network_SDN_Partitioned build_SDN_Partitioned_Network(Integer index, String failedLink){
		if (failedLink != null)
			System.out.println("SDN-partitioned network number " + index + ": Simulating Link Failure on Link " + failedLink);
		LinkedList<Router> theNodes = new LinkedList<Router>();
		for (String node : this.nodenames)
			if (bordernodeLists.get(index).contains(node))
				theNodes.add(new Router_SDN_Border(this.nodenames.indexOf(node), node));
			else
				theNodes.add(new Router_OSPF(this.nodenames.indexOf(node), node));
		this.buildBidirectionalLinks(theNodes, failedLink);
		Network_SDN_Partitioned network = new Network_SDN_Partitioned(theNodes, "Partitioned-" + index);
		if (failedLink != null)
			network.count_routing_reconfigurations_for(failedLink);
		return network;
	}
	
	public LinkedList<Network_SDN_Partitioned> build_SDN_Partitioned_Networks(String failedLink){
		LinkedList<Network_SDN_Partitioned> networks = new LinkedList<Network_SDN_Partitioned>();
		for (int index=0; index<this.bordernodeLists.size(); index++)
			networks.add(this.build_SDN_Partitioned_Network(index, failedLink));
		return networks;
	}
	
	public LinkedList<Network> buildAllNetworks(String failedLink) {
		LinkedList<Network> list = new LinkedList<Network>();
		list.add(this.build_OSPF_Network(failedLink));
		list.add(this.build_MPLS_Network(failedLink));
		list.addAll(this.build_Hybrid_Networks(failedLink));
		list.addAll(this.build_SDN_Partitioned_Networks(failedLink));
		return list;
	}
	
	public LinkedList<String> getLinkNames(){
		LinkedList<String> list = new LinkedList<String>();
		for (int i=0; i<this.nodenames.size()-1; i++)
			for (int j=i+1; j<this.nodenames.size(); j++)
				if (this.linkWeights.keySet().contains(nodenames.get(i)+"-"+nodenames.get(j)))
					list.add(nodenames.get(i)+"-"+nodenames.get(j));
		return list;
	}
	
	public String getFileName(){
		return this.fileName;
	}
	
	public int getNumberOfLinks() {
		return this.linkWeights.keySet().size() / 2;
	}
	
	public Network build(String networkType, String failedLink){
		if (networkType.equals("OSPF"))
			return this.build_OSPF_Network(failedLink);
		if (networkType.equals("MPLS"))
			return this.build_MPLS_Network(failedLink);
		if (networkType.startsWith("Hybrid-"))
			return this.build_Hybrid_Network(Integer.parseInt(networkType.split("-")[1]), failedLink);
		if (networkType.startsWith("Partitioned-"))
			return this.build_SDN_Partitioned_Network(Integer.parseInt(networkType.split("-")[1]), failedLink);
		throw new Error("ERROR: No such network type: " + networkType);
	}
	
}
