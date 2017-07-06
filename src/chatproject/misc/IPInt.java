package chatproject.misc;

public class IPInt 
{
	public static int packIP(byte[] bytes) 
	{
		  int val = 0;
		  for (int i = 0; i < bytes.length; i++) 
		  {
		    val <<= 8;
		    val |= bytes[i] & 0xff;
		  }
		  return val;
	}
	public static byte[] unpackIP(int bytes) 
	{
		  return new byte[] 
		  {
		    (byte)((bytes >>> 24) & 0xff),
		    (byte)((bytes >>> 16) & 0xff),
		    (byte)((bytes >>>  8) & 0xff),
		    (byte)((bytes       ) & 0xff)
		  };
	}
}
