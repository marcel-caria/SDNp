import java.util.LinkedList;

public class MultiMetaPath implements MultiPath{
	private LinkedList<MetaPath> metapaths;
	private int k = 15; // max number of MetaPaths for this source destination pair
	private Router_OSPF source;
	private Router_OSPF destination;
	
	@Override
	public int size(){
		return this.metapaths.size();
	}
	
	@Override
	public int getK() {
		return k;
	}

	@Override
	public void setK(int k) {
		this.k = k;
	}

	@Override
	public Router_OSPF getSource() {
		return source;
	}

	@Override
	public Router_OSPF getDestination() {
		return destination;
	}

	public MultiMetaPath(MetaPath initialPath){
		this.metapaths = new LinkedList<MetaPath>();
		this.metapaths.add(initialPath);
		this.source = (Router_OSPF) initialPath.getSource();
		this.destination = (Router_OSPF) initialPath.getDestination();
	}
	
	@Override
	public LinkedList<Path> getPaths(){
		LinkedList<Path> copy = new LinkedList<Path>(this.metapaths);
		return copy;
	}
	
	@Override
	public boolean add(Path newPath){
		if (!(newPath instanceof MetaPath))
			throw new Error("ERROR: " + newPath.toString() + " is not an instance of MetaPath");
		MetaPath newMetaPath = (MetaPath) newPath;
		if (this.metapaths.contains(newMetaPath))
			return false;
		if (!newMetaPath.getSource().equals(this.source))
			return false;
		if (!newMetaPath.getDestination().equals(this.destination))
			return false;
		if (this.isFull() && newMetaPath.length() >= this.length_of_longest_path())
			return false;
		int index = 0;
		for (MetaPath mp : this.metapaths)
			if (mp.length() < newMetaPath.length())
				index++;
		this.metapaths.add(index, newMetaPath);
		if (this.metapaths.size() > this.k)
			this.metapaths.removeLast();
		return true;
	}
	
	String getShortName(MetaPath metapath){
		return "MetaPath-" + metapaths.indexOf(metapath) + "-" + source.getName() + "-" + destination.getName();
	}
	
	@Override
	public String toString(){
		String out = "All MetaPaths from " + this.source.getName() + " to " + this.destination.getName() + ":\r\n";
		for (MetaPath p : this.metapaths)
			out += p.toString() + "\r\n";
		return out;
	}
	
	@Override
	public boolean isFull(){
		return this.metapaths.size() == this.k;
	}
	
	@Override
	public int length_of_longest_path(){
		return this.metapaths.getLast().length();
	}

	@Override
	public Path getPath(int index) {
		return this.metapaths.get(index);
	}

}
