exec(open('complete_doc.py',encoding='utf-8').read().split('for i, p in enumerate(doc.paragraphs):')[0])
from docx.table import Table
paras=list(doc.paragraphs)
titles=['INTRODUCTION TO JAVA','DATA TYPES, LOGICAL OPERATIONS, INPUT AND OUTPUT','CLASSES, OBJECTS AND ENCAPSULATION','INHERITANCE, POLYMORPHISM AND ABSTRACTION','EXCEPTION HANDLING','JAVA COLLECTION FRAMEWORK','MULTITHREADING, THREADS AND JAVA GUI WITH SWING','JDBC AND DATABASE PROGRAMMING']
for n in range(1,9):
 h=next(p for p in paras if p.text.strip()==f'Module {n}')
 replace_paragraph(h,f'Module {n}: {titles[n-1]}')
 idx=paras.index(h)
 replace_paragraph(paras[idx+1],module_content[n][0])
 # Use actual excerpts from the project's module guides, without altering the code.
 import re
 guide=(project/f'documentation/MODULE_{n}.md').read_text(encoding='utf-8')
 blocks=re.findall(r'```java\s*\n(.*?)```',guide,re.S)
 code=blocks[0]
 label=next(p for p in paras[idx+1:] if p.text.strip()=='Snippet Codes:')
 el=OxmlElement('w:p');label._p.addnext(el);p=Paragraph(el,label._parent)
 p.paragraph_format.line_spacing=1
 r=p.add_run(code.strip());r.font.name='Consolas';r.font.size=Pt(8)
summary=next(p for p in paras if 'Summarize the over-all' in p.text)
replace_paragraph(summary,'BUDGETLY is a personal budget tracker that helps users record income and expenses, set monthly spending limits, and review their balance and transaction history. Search, sorting, and budget warnings help users monitor spending. Java Swing provides its screens, while JDBC and Apache Derby store account and financial records. The project combines the concepts in Modules 1–8: Java fundamentals, data processing, encapsulated objects, inheritance and polymorphism, exception handling, collections, background threads, and database programming. Its purpose is to make everyday money management easier and demonstrate how these Java topics work together in a complete application.')
# Fill the original manual table in precisely the supplied two-column arrangement.
manual=doc.tables[1]
# Reuse the screenshot metadata from the previous builder without executing its edits.
text=Path('complete_doc.py').read_text(encoding='utf-8')
exec('screens = '+text.split('screens = ',1)[1].split('\n\n# Place captions',1)[0])
screens.append(('Documentation and learning guide','Open the User Manual tab to read instructions. The adjacent Java Modules 1–8 tab explains the project implementation.','codex-clipboard-59910a47-b75f-4fa5-82e9-c399bd85ba49.png'))
for i,(title,description,img) in enumerate(screens):
 row=manual.rows[0] if i==0 else manual.add_row()
 for c in row.cells:
  for child in list(c._tc):
   if child.tag.endswith('}p'):c._tc.remove(child)
 left,right=row.cells
 p=left.add_paragraph();r=p.add_run(f'Figure 1.{i+1}  {title.upper()}');r.bold=True
 left.add_paragraph(description)
 p=right.add_paragraph(f'Figure 1.{i+1}')
 from PIL import Image
 path=Path(r'C:\Users\wende\AppData\Local\Temp')/img
 with Image.open(path) as im: w=min(3.0,4.8*im.width/im.height)
 right.add_paragraph().add_run().add_picture(str(path),width=Inches(w))
# Populate full Java source code under each requested set of labels.
name=next(p for p in paras if p.text.strip()=='File Name:')
anchor=name
for p in paras[paras.index(name):paras.index(name)+3]:
 p.clear()
for file in sorted((project/'src').rglob('*.java')):
 description={'Main':'Starts the application and initializes the database.','MainFrame':'Provides the regular user dashboard and financial management screens.','LoginFrame':'Provides login and registration screens.','AdminDashboard':'Provides administrator account management.','BudgetService':'Validates and processes financial data.','AuthService':'Validates accounts and handles authentication.'}.get(file.stem,'Defines '+file.stem+' records and behavior.' if 'model' in str(file) else 'Provides database operations for '+file.stem+'.')
 for label,value in [('File Name: ',str(file.relative_to(project))),('File Description: ',description),('Source Codes:','')]:
  el=OxmlElement('w:p');anchor._p.addnext(el);p=Paragraph(el,anchor._parent);p.add_run(label).bold=True;p.add_run(value);anchor=p
 for line in file.read_text(encoding='utf-8-sig').splitlines():
  el=OxmlElement('w:p');anchor._p.addnext(el);p=Paragraph(el,anchor._parent);p.paragraph_format.line_spacing=1;p.paragraph_format.space_after=Pt(0);r=p.add_run(line.expandtabs(4));r.font.name='Consolas';r.font.size=Pt(8);anchor=p
out=Path(r'C:\Users\wende\Downloads\CashTrack - Budget Tracker - Revised.docx')
doc.save(out)
print(out)
