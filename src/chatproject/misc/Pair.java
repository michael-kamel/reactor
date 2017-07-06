package chatproject.misc;

public class Pair<L,R> 
{
    private L left;
    private R right;
    
    public Pair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }
    
    public L getLeft()
    { 
    	return left;
    }
    public R getRight()
    { 
    	return right;
    }
    public void setL(L left)
    {
    	this.left = left;
    }
    public void setR(R right)
    { 
    	this.right = right;
    }
}