import math
from tables import TA,TB
print(len(TA),len(TB), len({tuple(r[:4]) for r in TA}), len({tuple(r[:4]) for r in TB}))
def rad(x): return math.radians(x%360)
def moon(jde):
    T=(jde-2451545.0)/36525
    Lp=218.3164477+481267.88123421*T-0.0015786*T**2+T**3/538841-T**4/65194000
    D=297.8501921+445267.1114034*T-0.0018819*T**2+T**3/545868-T**4/113065000
    M=357.5291092+35999.0502909*T-0.0001536*T**2+T**3/24490000
    Mp=134.9633964+477198.8675055*T+0.0087414*T**2+T**3/69699-T**4/14712000
    F=93.2720950+483202.0175233*T-0.0036539*T**2-T**3/3526000+T**4/863310000
    A1=119.75+131.849*T; A2=53.09+479264.290*T; A3=313.45+481266.484*T
    E=1-0.002516*T-0.0000074*T*T
    sl=sr=sb=0.0
    for d,m,mp,f,cl,cr in TA:
        e=E**abs(m)
        a=rad(d*D+m*M+mp*Mp+f*F)
        sl+=cl*e*math.sin(a); sr+=cr*e*math.cos(a)
    for d,m,mp,f,cb in TB:
        e=E**abs(m)
        sb+=cb*e*math.sin(rad(d*D+m*M+mp*Mp+f*F))
    sl+=3958*math.sin(rad(A1))+1962*math.sin(rad(Lp-F))+318*math.sin(rad(A2))
    sb+=-2235*math.sin(rad(Lp))+382*math.sin(rad(A3))+175*math.sin(rad(A1-F))+175*math.sin(rad(A1+F))+127*math.sin(rad(Lp-Mp))-115*math.sin(rad(Lp+Mp))
    return (Lp+sl/1e6)%360, sb/1e6, 385000.56+sr/1000, sl, sr, sb
print(moon(2448724.5))
print("expected lam(before nutation) 133.162655  beta -3.229126  delta 368409.7  sumL -1127527 sumR -16590875 sumB -3229126")
