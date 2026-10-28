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
 static class Feature{long stake;int left;Feature(long stake,int count){this.stake=stake;left=count;}long next(long balance){if(left<=0)throw new IllegalStateException("No free spin");left--;return balance;}void result(long payout,boolean scatter){if(payout<0)throw new IllegalArgumentException();/* No retrigger by design. */}}
 static int[][] none(){return new int[][]{{1,2,3,4,5},{2,3,4,5,0},{0,5,1,2,3}};}
 @Test public void regularGridsAgreeExactlyWithProduction(){Random rng=new Random(9284);for(int i=0;i<10000;i++){int[][] g=new int[3][5];for(int[] row:g)for(int c=0;c<5;c++)row[c]=SlotsView.pickSymbol(rng);for(long bet:new long[]{5,10,25,200})assertEquals(SlotsView.evaluate(g,bet,null),evaluate(g,bet));}}
 @Test public void realSubstitutionAndScatterExclusion(){int[][] g={{1,2,3,4,5},{0,WILD,0,2,1},{5,4,3,2,1}};assertEquals(96,evaluate(g,10));g[1][1]=SCATTER;assertEquals(0,evaluate(g,10));}
 @Test public void strongestLineAwardOnceNotSixTimes(){int[][] g={{SCATTER,SCATTER,SCATTER,SCATTER,SCATTER},{0,WILD,WILD,WILD,0},{SCATTER,SCATTER,SCATTER,SCATTER,SCATTER}};assertEquals(1200,evaluate(g,10));}
 @Test public void scatterCountsDistinctReels(){int[][] g=none();g[0][0]=g[1][0]=g[2][0]=SCATTER;assertFalse(bonus(g));g[0][1]=g[0][2]=SCATTER;assertTrue(bonus(g));}
 @Test public void anticipationRequiresTwoStoppedScatterReelsAndHonorsStop(){int[][] g=none();g[0][0]=g[1][1]=SCATTER;assertFalse(anticipation(g,1,false));assertTrue(anticipation(g,2,false));assertFalse(anticipation(g,2,true));assertFalse(anticipation(g,5,false));g[0][2]=SCATTER;assertFalse(anticipation(g,3,false));}
 @Test public void freeSpinNeverChargesOrRetriggers(){Feature f=new Feature(25,5);long balance=500;for(int i=0;i<5;i++){assertEquals(balance,f.next(balance));f.result(0,true);}assertEquals(0,f.left);assertEquals(25,f.stake);try{f.next(balance);fail();}catch(IllegalStateException expected){}}
}
