public class PathElement{
	
	private IP_Path path;
	private Link link;
	
	public PathElement(IP_Path path){
		this.path = path;
	}
	
	public PathElement(Link link){
		this.link = link;
	}
	
	public Integer length(){
		if (this.path == null)
			return 2;
		return this.path.length();
	}
	
	@Override
	public String toString(){
		if (this.path == null)
			return this.link.toString();
		return this.path.toString();
	}
	
	Router getSource(){
		if (this.path == null)
			return this.link.getSource();
		return this.path.getSource();
	}
	
	Router getDestination(){
		if (this.path == null)
			return this.link.getDestination();
		return this.path.getDestination();
	}
	
	public boolean isLink(){
		if (this.link != null)
			return true;
		return false;
	}
	
	public boolean isPath(){
		if (this.path != null)
			return true;
		return false;
	}
	
	public IP_Path getPath(){
		return this.path;
	}
	
	public Link getLink(){
		return this.link;
	}
	
	@Override
	public boolean equals(Object other){
		if (other == this) return true;
		if (other == null) return false;
		if (other.getClass() != this.getClass()) return false;
		PathElement otherPathElement = (PathElement) other;
		if (this.isLink() && otherPathElement.isLink())
			return this.getLink().equals(otherPathElement.getLink());
		if (this.isPath() && otherPathElement.isPath())
			return this.getPath().equals(otherPathElement.getPath());
		return false;
	}
	
	@Override
	public int hashCode(){
		String signature = "PathElement";
		if (this.isPath())
			signature += this.getPath().toString();
		if (this.isLink())
			signature += this.getLink().toString();
		return signature.hashCode();
	}
	
}
	
