package nir.com.neonvegasslots;
import static org.junit.Assert.*;
import java.util.Random;
import org.junit.Test;
public class PayoutTest {
 private static int[][] fill(int s){ int[][] g=new int[3][5]; for(int[] r:g) java.util.Arrays.fill(r,s); return g; }
 @Test public void knownGridsPayExactly(){
  // all 5 lines of symbol 0 (dragon): 5 lines x PAY5[0]=1200 at bet 10 -> 1200 each
  assertEquals(6000,SlotsView.evaluate(fill(0),10,null));
  assertEquals("pay scales with bet",60000,SlotsView.evaluate(fill(0),100,null));
  int[][] g={{1,2,3,4,5},{0,0,0,2,1},{5,4,3,2,1}}; // middle line: three dragons then break -> PAY3[0]=96
  assertEquals(96,SlotsView.evaluate(g,10,null));
  int[][] none={{1,2,3,4,5},{2,3,4,5,0},{0,5,1,2,3}};
  assertEquals(0,SlotsView.evaluate(none,10,null));
 }
 @Test public void reportRtpFromRealCode(){
  Random r=new Random(7); long n=4000000,tot=0; int hit=0,x25=0,x5=0,x10=0,x20=0,x50=0,x100=0; long max=0;
  for(long i=0;i<n;i++){ int[][] g=new int[3][5]; for(int a=0;a<3;a++)for(int b=0;b<5;b++)g[a][b]=SlotsView.pickSymbol(r);
   long w=SlotsView.evaluate(g,100,null); tot+=w; if(w>0)hit++; if(w>=500)x5++; if(w>=2500)x25++; if(w>=1000)x10++; if(w>=2000)x20++; if(w>=5000)x50++; if(w>=10000)x100++; if(w>max)max=w; }
  double rtp=tot/(double)(n*100);
  System.out.println("RTP_REAL_CODE rtp="+rtp+" hit="+(hit/(double)n)+" ge5x="+x5+" ge10x="+x10+" ge20x="+x20+" ge25x="+x25+" ge50x="+x50+" ge100x="+x100+" max="+(max/100.0)+"x n="+n);
  assertTrue("RTP in approved band, was "+rtp, rtp>=0.92 && rtp<=0.955);
  assertTrue("sanity: 25x wins must occur in simulation with frequency of at least 1 in 1000 spins on average (measured 1 in "+(n/Math.max(1,x25))+"); this is a long-run average, not a guarantee for any player or session", x25*1000L>=n);
 }
}
