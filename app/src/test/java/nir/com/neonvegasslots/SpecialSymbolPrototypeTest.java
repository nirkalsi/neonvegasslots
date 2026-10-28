package nir.com.neonvegasslots;
import static org.junit.Assert.*;import java.util.*;import org.junit.Test;
/** Isolated test-only design prototype. Never packaged in Android runtime. */
public class SpecialSymbolPrototypeTest {
 static final int WILD=6,SCATTER=7;
 static final int[][] LINES={{1,1,1,1,1},{0,0,0,0,0},{2,2,2,2,2},{0,1,2,1,0},{2,1,0,1,2}};
 static long evaluate(int[][] grid,long bet){long total=0;for(int[] line:LINES){long best=0;for(int symbol=0;symbol<6;symbol++){int run=0;for(int col=0;col<5;col++){int s=grid[line[col]][col];if(s==symbol||s==WILD)run++;else break;}if(run>=3)best=Math.max(best,SlotsView.symbolPayout(symbol,run)*bet/10);}total+=best;}return total;}
 static int scatterReels(int[][] grid,int stoppedColumns){int n=0;for(int c=0;c<stoppedColumns;c++){boolean any=false;for(int r=0;r<3;r++)any|=grid[r][c]==SCATTER;if(any)n++;}return n;}
 static boolean bonus(int[][] grid){return scatterReels(grid,5)>=3;}
 static boolean anticipation(int[][] grid,int stoppedColumns,boolean quickStop){return !quickStop&&stoppedColumns<5&&scatterReels(grid,stoppedColumns)==2;}
 static class Feature{final long stake;int left;boolean inFlight;long totalAward;Feature(long stake,int count){if(stake<=0||count<0)throw new IllegalArgumentException();this.stake=stake;left=count;}long next(long balance){if(left<=0||inFlight)throw new IllegalStateException("No available free spin");left--;inFlight=true;return balance;}long result(long balance,long payout,boolean scatter){if(!inFlight||payout<0)throw new IllegalStateException();inFlight=false;totalAward+=payout;return balance+payout;/* No retrigger by design. */}String save(){return stake+":"+left+":"+inFlight+":"+totalAward;}static Feature restore(String saved){String[] x=saved.split(":");Feature f=new Feature(Long.parseLong(x[0]),Integer.parseInt(x[1]));f.inFlight=Boolean.parseBoolean(x[2]);f.totalAward=Long.parseLong(x[3]);return f;}}

 static int[][] none(){return new int[][]{{1,2,3,4,5},{2,3,4,5,0},{0,5,1,2,3}};}
 @Test public void regularGridsAgreeExactlyWithProduction(){Random rng=new Random(9284);for(int i=0;i<10000;i++){int[][] g=new int[3][5];for(int[] row:g)for(int c=0;c<5;c++)row[c]=SlotsView.pickSymbol(rng);for(long bet:new long[]{5,10,25,200})assertEquals(SlotsView.evaluate(g,bet,null),evaluate(g,bet));}}
 @Test public void realSubstitutionAndScatterExclusion(){int[][] g={{1,2,3,4,5},{0,WILD,0,2,1},{5,4,3,2,1}};assertEquals(96,evaluate(g,10));g[1][1]=SCATTER;assertEquals(0,evaluate(g,10));}
 @Test public void strongestLineAwardOnceNotSixTimes(){int[][] g={{SCATTER,SCATTER,SCATTER,SCATTER,SCATTER},{0,WILD,WILD,WILD,0},{SCATTER,SCATTER,SCATTER,SCATTER,SCATTER}};assertEquals(1200,evaluate(g,10));}
 @Test public void scatterCountsDistinctReels(){int[][] g=none();g[0][0]=g[1][0]=g[2][0]=SCATTER;assertFalse(bonus(g));g[0][1]=g[0][2]=SCATTER;assertTrue(bonus(g));}
 @Test public void anticipationRequiresTwoStoppedScatterReelsAndHonorsStop(){int[][] g=none();g[0][0]=g[1][1]=SCATTER;assertFalse(anticipation(g,1,false));assertTrue(anticipation(g,2,false));assertFalse(anticipation(g,2,true));assertFalse(anticipation(g,5,false));g[0][2]=SCATTER;assertFalse(anticipation(g,3,false));}
 @Test public void freeSpinNeverChargesOrRetriggers(){Feature f=new Feature(25,5);long balance=500;for(int i=0;i<5;i++){assertEquals(balance,f.next(balance));balance=f.result(balance,0,true);}assertEquals(0,f.left);assertEquals(25,f.stake);try{f.next(balance);fail();}catch(IllegalStateException expected){}}
 @Test public void bonusAwardCreditsOnceAndCannotDoubleStart(){Feature f=new Feature(10,5);long b=f.next(500);try{f.next(b);fail();}catch(IllegalStateException expected){}assertEquals(550,f.result(b,50,true));assertEquals(4,f.left);try{f.result(550,50,false);fail();}catch(IllegalStateException expected){}assertEquals(50,f.totalAward);}
 @Test public void restoreKeepsStakeRemainingAndInFlightWithoutDoubleCredit(){Feature f=new Feature(25,5);long b=f.next(500);Feature r=Feature.restore(f.save());assertEquals(25,r.stake);assertEquals(4,r.left);assertTrue(r.inFlight);b=r.result(b,75,true);assertEquals(575,b);Feature done=Feature.restore(r.save());assertEquals(75,done.totalAward);assertFalse(done.inFlight);assertEquals(575,done.next(b));assertEquals(3,done.left);}
}
