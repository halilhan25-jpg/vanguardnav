import sys,xml.etree.ElementTree as E
from pathlib import Path
p=Path(sys.argv[1]);ns='http://schemas.android.com/apk/res/android';E.register_namespace('android',ns)
a='{'+ns+'}';root=E.parse(p);m=root.getroot();app=m.find('application')
for perm in ('ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION','FOREGROUND_SERVICE','FOREGROUND_SERVICE_LOCATION'):
 E.SubElement(m,'uses-permission',{a+'name':'android.permission.'+perm})
E.SubElement(m,'uses-permission',{a+'name':'androidx.car.app.MAP_TEMPLATES'})
E.SubElement(app,'service',{a+'name':'.TrailService',a+'foregroundServiceType':'location',a+'exported':'false'})
E.SubElement(app,'meta-data',{a+'name':'com.google.android.gms.car.application',a+'resource':'@xml/automotive_app_desc'})
E.SubElement(app,'meta-data',{a+'name':'androidx.car.app.minCarApiLevel',a+'value':'1'})
E.SubElement(m,'uses-permission',{a+'name':'androidx.car.app.ACCESS_SURFACE'})
service=E.SubElement(app,'service',{a+'name':'.PlacesService',a+'exported':'true'})
f=E.SubElement(service,'intent-filter')
E.SubElement(f,'action',{a+'name':'androidx.car.app.CarAppService'})
E.SubElement(f,'category',{a+'name':'androidx.car.app.category.POI'})
root.write(p,encoding='utf-8',xml_declaration=True)
(p.parent/'res/xml/automotive_app_desc.xml').write_text('<automotiveApp><uses name="template"/></automotiveApp>')
g=Path('android/app/build.gradle')
g.write_text(g.read_text()+"\ndependencies { implementation 'androidx.car.app:app:1.7.0'; implementation 'androidx.car.app:app-projected:1.7.0' }\n")
