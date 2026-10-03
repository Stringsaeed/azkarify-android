"""Generate Android UI resources and migrate inline bilingual UI labels.

Resources remain the translation source after the first migration. Bundled
English/Arabic devotional JSON is content and is deliberately left unchanged.
Run this after importing the iOS assets to refresh shared UI copy and routines.
"""
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
LITERAL=re.compile(r'"(?:\\.|[^"\\])*"')
def decode(value):
    return json.loads(value.replace('\\$', '$'))
def literal(value):
    return json.dumps(value,ensure_ascii=False).replace('$','\\$')
def calls(text,name):
    for match in re.finditer(re.escape(name)+r'\(',text):
        start=match.end(); pos=start; depth=1; quoted=False; escaped=False
        while depth:
            c=text[pos]
            if c=='"' and not escaped: quoted=not quoted
            if not quoted:
                if c=='(': depth+=1
                elif c==')': depth-=1
            escaped=c=='\\' and not escaped; pos+=1
        yield match.start(),pos,text[start:pos-1]
def arguments(body):
    parts=[]; start=0; depth=0; quoted=False; escaped=False
    for i,c in enumerate(body):
        if c=='"' and not escaped: quoted=not quoted
        if not quoted:
            if c in '([{': depth+=1
            elif c in ')]}': depth-=1
            elif c==',' and depth==0: parts.append(body[start:i].strip());start=i+1
        escaped=c=='\\' and not escaped
    parts.append(body[start:].strip())
    return [x for x in parts if x]
def android_value(value):
    return '"'+value.replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\t','\\t')+'"'
def read_xml(path):
    if not path.exists(): return {}
    result={}
    for node in ET.parse(path).getroot().findall('string'):
        text=node.text or ''
        if text.startswith('"') and text.endswith('"'): text=text[1:-1]
        result[node.attrib['name']]=text.replace('\\n','\n').replace('\\t','\t').replace('\\"','"').replace('\\\\','\\')
    return result

def export():
    translations={}
    # On subsequent imports, retain native Android additions from the resource catalog.
    registry=MAIN/'java/com/stringsaeed/azkarify/data/UiStrings.kt'
    if registry.exists():
        en=read_xml(MAIN/'res/values/strings.xml');ar=read_xml(MAIN/'res/values-ar/strings.xml')
        for key,res in re.findall(r'("(?:\\.|[^"\\])*")\s+to\s+R\.string\.(\w+)',registry.read_text()):
            translations[decode(key.strip())]=(en[res],ar[res])
    sources={}
    for lang in ['en','ar']:
        p=MAIN/f'assets/copy_{lang}.json'
        sources[lang]=json.loads(p.read_text()) if p.exists() else {}
    for key,value in sources['en'].items():
        translations.setdefault(key,(value,sources['ar'].get(key,value)))
    def pair(en,ar):
        key=en
        existing=translations.get(key)
        if existing and existing[1] not in [en,ar]:
            key='ui_'+hashlib.sha256((en+'|'+ar).encode()).hexdigest()[:12]
        translations[key]=(en,ar)
        return key
    for path in (MAIN/'java').rglob('*.kt'):
        if path.name=='UiStrings.kt':continue
        text=path.read_text(); replacements=[]
        for start,end,body in calls(text,'state.text'):
            args=arguments(body)
            if len(args)==2:
                english=list(LITERAL.finditer(args[0]));arabic=list(LITERAL.finditer(args[1]))
                if len(english)!=len(arabic):raise ValueError(f'Unsupported localization expression: {path}: {body}')
                expression=args[0]
                for e,a in reversed(list(zip(english,arabic))):
                    key=pair(decode(e.group()),decode(a.group()))
                    expression=expression[:e.start()]+literal(key)+expression[e.end():]
                replacements.append((start,end,'state.text('+expression+')'))
        for start,end,value in reversed(replacements):text=text[:start]+value+text[end:]
        path.write_text(text)
    # Routine structure stays in JSON; labels resolve through Android resources.
    p=MAIN/'assets/journeys.json';journeys=json.loads(p.read_text())
    for journey in journeys:
        if 'en' not in journey:continue
        journey['title']=pair(journey.pop('en'),journey.pop('ar'))
        journey['subtitle']=pair(journey.pop('subtitleEn'),journey.pop('subtitleAr'))
        for step in journey['steps']:
            step['title']=pair(step.pop('en'),step.pop('ar'))
            step['note']=pair(step.pop('noteEn'),step.pop('noteAr'))
            for option in step['options']:option['title']=pair(option.pop('en'),option.pop('ar'))
    p.write_text(json.dumps(journeys,ensure_ascii=False,indent=2)+'\n')
    # Convert prayer method labels into resource keys.
    p=MAIN/'java/com/stringsaeed/azkarify/services/PrayerSchedule.kt';text=p.read_text()
    def method_label(m):return literal(pair(decode(m[1]),decode(m[2])))
    text=re.sub(r'\(("(?:\\.|[^"\\])*")\s+to\s+("(?:\\.|[^"\\])*")\)',method_label,text)
    p.write_text(text)
    # Extract the remaining in-code prayer routine labels before replacing Bilingual.
    p=MAIN/'java/com/stringsaeed/azkarify/data/AzkarRepository.kt';text=p.read_text()
    names={'Fajr':'الفجر','Dhuhr':'الظهر','Asr':'العصر','Maghrib':'المغرب','Isha':'العشاء'}
    for en,ar in names.items():pair(en+' journey','رحلة '+ar)
    for _,_,body in calls(text,'Bilingual'):
        args=arguments(body)
        if len(args)==2 and all(LITERAL.fullmatch(x) for x in args):
            en,ar=map(decode,args)
            if '$' not in en:pair(en,ar)
    # Notification text uses the same resource catalog.
    pair('🌅 Morning reminder','🌅 تذكير الصباح');pair('🌙 Evening reminder','🌙 تذكير المساء')
    pair('Time for morning azkar','حان وقت أذكار الصباح');pair('Time for evening azkar','حان وقت أذكار المساء')
    translations.setdefault('No results',('No results','لا توجد نتائج'))
    translations['Azkarify']=('Azkarify','أذكاريفاي')
    translations['English']=('English','English')
    translations['Arabic']=('العربية','العربية')
    translations['Azkar reminders']=('Azkar reminders','تذكير الأذكار')
    translations.pop('',None)
    entries=[]
    for key,(en,ar) in sorted(translations.items()):
        slug=re.sub(r'[^a-z0-9]+','_',en.lower()).strip('_')[:42] or 'label'
        resource='app_'+slug+'_'+hashlib.sha256(key.encode()).hexdigest()[:8]
        entries.append((key,resource,en,ar))
    for lang,index in [('en',2),('ar',3)]:
        folder=MAIN/('res/values' if lang=='en' else 'res/values-ar');folder.mkdir(parents=True,exist_ok=True)
        lines=['<resources>','    <string name="app_name">Azkarify</string>']
        for entry in entries:lines.append(f'    <string name="{entry[1]}" formatted="false">{escape(android_value(entry[index]))}</string>')
        (folder/'strings.xml').write_text('\n'.join(lines+['</resources>'])+'\n')
    registry.write_text('''package com.stringsaeed.azkarify.data

import android.content.Context
import android.content.res.Configuration
import com.stringsaeed.azkarify.R
import java.util.Locale

// Generated by scripts/android_localization.py from Android resource translations.
internal val uiStringIds = mapOf(
'''+''.join('    '+literal(k)+' to R.string.'+res+',\n' for k,res,_,_ in entries)+''')

fun Context.uiStrings(language: String): Map<String,String> {
    val configuration=Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    val localized=createConfigurationContext(configuration).resources
    return uiStringIds.mapValues { (_,id) -> localized.getString(id) }
}
''')
    for lang in ['en','ar']:
        p=MAIN/f'assets/copy_{lang}.json'
        if p.exists():p.unlink()
    print(f'Generated {len(entries)} bilingual Android UI strings.')

if __name__=='__main__':export()
