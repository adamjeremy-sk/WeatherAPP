import math
from oracle_47a import moon
def jd(y,mo,d,h,mi):
    if mo<=2: y-=1; mo+=12
    A=y//100; B=2-A+A//4
    return int(365.25*(y+4716))+int(30.6001*(mo+1))+d+B-1524.5+(h+mi/60)/24
def sun_lon(jde):
    T=(jde-2451545.0)/36525
    L0=280.46646+36000.76983*T+0.0003032*T*T
    M=math.radians(357.52911+35999.05029*T-0.0001537*T*T)
    C=(1.914602-0.004817*T-0.000014*T*T)*math.sin(M)+(0.019993-0.000101*T)*math.sin(2*M)+0.000289*math.sin(3*M)
    R=1.000001018*(1-(0.016708634-0.000042037*T)**2)/(1+0.016708634*math.cos(M+math.radians(C)))
    return (L0+C-0.00569*0 -20.4898/3600/R)%360   # true longitude minus aberration, no nutation
DT=69.0
def elong(jdutc):
    j=jdutc+DT/86400
    return (moon(j)[0]-sun_lon(j))%360
targets={'LQ 11-01 20:28':(2026,11,1,20,28,270),'NM 11-09 07:02':(2026,11,9,7,2,0),'FQ 11-17 11:47':(2026,11,17,11,47,90),'FM 11-24 14:53':(2026,11,24,14,53,180)}
for k,(y,mo,d,h,mi,tgt) in targets.items():
    j=jd(y,mo,d,h,mi)
    # find root of elong-tgt via secant
    f=lambda x:((elong(x)-tgt+180)%360)-180
    a,b=j-0.05,j+0.05
    for _ in range(40):
        fa,fb=f(a),f(b)
        if fb==fa: break
        c=b-fb*(b-a)/(fb-fa); a,b=b,c
    print(k,"computed - listed = %.2f min"%((b-j)*1440))
