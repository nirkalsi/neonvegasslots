package nir.com.neonvegasslots;

/** Disabled visual candidate. Phase is measured in symbol pitches, not pixels. */
final class ReelLandingMotion {
    private long start, end;
    private double from, to, speed;
    ReelLandingMotion(long start,long end,int column){this.start=start;this.end=end;to=6+3*column;}
    double phase(long now){
        double t=Math.max(0,Math.min(1,(now-start)/(double)Math.max(1,end-start)));
        double t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*from+(t3-2*t2+t)*(end-start)*speed+(-2*t3+3*t2)*to;
    }
    double velocity(long now){
        double d=Math.max(1,end-start),t=Math.max(0,Math.min(1,(now-start)/d));
        return ((6*t*t-6*t)*from+(3*t*t-4*t+1)*d*speed+(-6*t*t+6*t)*to)/d;
    }
    void deadline(long now,long newEnd){
        if(newEnd==end)return;
        double position=phase(now),v=Math.max(0,velocity(now));
        start=now;end=Math.max(now+1,newEnd);from=position;speed=v;
        // Hermite's monotone bound: initial slope <= three times the secant.
        to=3*Math.ceil((from+Math.max(.01,v*(end-start)/3))/3);
    }
}
