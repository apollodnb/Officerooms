from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
import sys
F='/home/claude/src0/OfficeRooms/tools/promo/'
S=1024
src=Image.open(sys.argv[1]).convert('RGB')
w,h=src.size; cx=int(w*float(sys.argv[2])); side=h
bg=src.crop((cx-side//2,0,cx+side//2,side)).resize((S,S),Image.LANCZOS)
bg=ImageEnhance.Brightness(bg).enhance(float(sys.argv[3]))
tint=Image.new('RGB',(S,S),(120,95,40)); bg=Image.blend(bg,tint,0.28)
# vignette
v=Image.new('L',(S,S),0); d=ImageDraw.Draw(v); d.ellipse((-S*0.25,-S*0.2,S*1.25,S*1.2),fill=255)
v=v.filter(ImageFilter.GaussianBlur(160)); black=Image.new('RGB',(S,S),(10,8,4)); bg=Image.composite(bg,black,v)
# subtle scanlines
sl=Image.new('RGBA',(S,S),(0,0,0,0)); sd=ImageDraw.Draw(sl)
for y in range(0,S,4): sd.line((0,y,S,y),fill=(0,0,0,28))
img=bg.convert('RGBA'); img.alpha_composite(sl)
GOLD=(232,200,96); CREAM=(244,236,218)
big=ImageFont.truetype(F+'BebasNeue-Regular.ttf',330); mono=ImageFont.truetype(F+'IBMPlexMono-Medium.ttf',46)
def ctext(layer,y,t,font,fill,spacing=0):
    dd=ImageDraw.Draw(layer)
    if spacing:
        ws=[dd.textlength(c,font=font) for c in t]; tot=sum(ws)+spacing*(len(t)-1); x=(S-tot)/2
        for c,cw in zip(t,ws): dd.text((x,y),c,font=font,fill=fill); x+=cw+spacing
    else:
        tw=dd.textlength(t,font=font); dd.text(((S-tw)/2,y),t,font=font,fill=fill)
txt=Image.new('RGBA',(S,S),(0,0,0,0))
ctext(txt,190,'OFFICE',big,CREAM,6); ctext(txt,500,'ROOMS',big,CREAM,6)
glow=txt.filter(ImageFilter.GaussianBlur(22)); a=glow.split()[3].point(lambda p:int(p*0.55))
gl=Image.new('RGBA',(S,S),(255,214,120,0)); gl.putalpha(a)
sh=Image.new('RGBA',(S,S),(0,0,0,0)); sh.putalpha(txt.split()[3]); sh=sh.transform((S,S),Image.AFFINE,(1,0,-7,0,1,-7))
img.alpha_composite(gl); img.alpha_composite(sh); img.alpha_composite(txt)
d=ImageDraw.Draw(img)
# plaque
pt='YOU NOCLIPPED'; tw=sum(d.textlength(c,font=mono) for c in pt)+8*(len(pt)-1); px0=(S-tw)/2-28; px1=(S+tw)/2+28
d.rectangle((px0,92,px1,166),fill=(176,140,58),outline=(110,84,28),width=4); d.rectangle((px0+9,101,px1-9,157),outline=(92,70,22),width=2)
ctext(img,99,pt,mono,(52,38,10),8)
# rule + frame
d.rectangle((S/2-110,862,S/2+110,872),fill=GOLD)
ctext(img,900,'MINECRAFT 1.20.1  FORGE',ImageFont.truetype(F+'IBMPlexMono-Medium.ttf',40),GOLD,4)
d.rectangle((0,0,S-1,S-1),outline=GOLD,width=14)
out=img.convert('RGB'); out.save(sys.argv[4]); out.resize((400,400),Image.LANCZOS).save(sys.argv[4].replace('.png','_400.png'))
