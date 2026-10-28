package nir.com.neonvegasslots;
import static org.junit.Assert.*;import org.junit.Test;
public class ReelLandingMotionTest {
 @Test public void wholeCycleAndMonotone(){for(int c=0;c<5;c++){ReelLandingMotion m=new ReelLandingMotion(0,600+c*150,c);check(m,0,600+c*150);}}
 @Test public void shorteningAndExtensionPreservePhaseAndSpeed(){for(long changed:new long[]{40,220,450,580})for(long duration:new long[]{110,500,1100}){ReelLandingMotion m=new ReelLandingMotion(0,600,0);double p=m.phase(changed),v=m.velocity(changed);m.deadline(changed,changed+duration);assertEquals(p,m.phase(changed),1e-9);assertEquals(v,m.velocity(changed),1e-9);check(m,changed,changed+duration);}}
 @Test public void repeatedDeadlineChangesRemainContinuous(){ReelLandingMotion m=new ReelLandingMotion(0,1200,4);for(long t=100;t<500;t+=40){double p=m.phase(t),v=m.velocity(t);m.deadline(t,t+110);assertEquals(p,m.phase(t),1e-9);assertEquals(v,m.velocity(t),1e-9);}check(m,460,570);}
 private void check(ReelLandingMotion m,long start,long end){double last=m.phase(start);for(long t=start;t<=end;t++){double p=m.phase(t);assertTrue(p+1e-8>=last);assertTrue(m.velocity(t)>=-1e-8);last=p;}assertEquals(0,m.phase(end)%3,1e-8);assertEquals(0,m.velocity(end),1e-8);}
}
