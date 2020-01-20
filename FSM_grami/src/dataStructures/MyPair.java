package dataStructures;

//ALE
class MyPair 
{
	String key;
	int inDegree;
	
	public MyPair(String key, int inDegree)
	{
		this.key = key;
		this.inDegree = inDegree;
	}
	
	public void addToInDegree()
	{
		inDegree = inDegree + 1;
	}
	
	public int getInDegree()
	{
		return inDegree;
	}
	
	public String getLabel()
	{
		return key;
	}
}
