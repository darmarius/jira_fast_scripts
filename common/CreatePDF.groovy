import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean
import org.apache.commons.io.FilenameUtils
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.fields.CustomField

ApplicationUser jiraServiceUser  = ComponentAccessor.userManager.getUserByName('Admin')

CustomField bicCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("BIC").first()
CustomField bankNameCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Bank's Name").first()
CustomField corrAccCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Correspondent account").first()
CustomField recipAccCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Recipient's Account Number").first()
CustomField recipNameCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Recipient").first()

Map mapFields = new HashMap()
mapFields.put("БИК:",bicCF.getValue(issue))
mapFields.put("Банк получателя:",bankNameCF.getValue(issue))
mapFields.put("Город отделения банка:",bankCityCF.getValue(issue))
mapFields.put("Корреспондентский счет:",corrAccCF.getValue(issue))
mapFields.put("Лицевой счет получателя:",recipAccCF.getValue(issue))
mapFields.put("Получатель (Ф.И.О.):",recipNameCF.getValue(issue))
createDocument(jiraServiceUser,issue,mapFields)   


void createDocument (ApplicationUser user, MutableIssue issue, Map mapFields){
// Page parameters
    int marginLeftCm = 5
    int marginRightCm = 5
    int marginTopCm = 5
    int marginBottomCm = 5
// 1 point = 1/72 inch ~ 1/2.8 mm

    BaseFont baseFont = BaseFont.createFont("/tmp/calibri.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    BaseFont boldBaseFont = BaseFont.createFont("/tmp/calibri.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

    Font font = new Font(baseFont,10.0f)
    Font boldFont = new Font(boldBaseFont,10.0f,Font.BOLD)

    Document document = new Document(
        PageSize.A4, (marginLeftCm*2.8).toInteger(), (marginRightCm*2.8).toInteger(), (marginTopCm*2.8).toInteger(), (marginBottomCm*2.8).toInteger()
    )

    Chapter chapter = new Chapter(new Paragraph(),1)
    chapter.setNumberDepth(0)
    chapter.add(newRightParagraph("Главному бухгалтеру", font))
    chapter.add(newRightParagraph("Компания нейм", font))
    chapter.add(newRightParagraph("ФИО", font))
    chapter.add(newRightParagraph("от ${mapFields.getAt("Получатель (Ф.И.О.):")}", font))
    chapter.add(Chunk.NEWLINE)
    chapter.add(newCenteredParagraph("ЗАЯВЛЕНИЕ", boldFont))
    chapter.add(Chunk.NEWLINE)
    chapter.add(newCenteredParagraph(desc, font))
    chapter.add(Chunk.NEWLINE)

    PdfPTable table 
	table = new PdfPTable(2)
    table.setWidthPercentage(90)
    table.setWidths(30,70)
    table.setSpacingBefore(5)
    table.setSpacingAfter(5)

    PdfPCell cell 
    mapFields.each { K,V->
        cell = newCell(table,(String)K, font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.BOX,1,1)
        cell = newCell(table,(String)V, font,Element.ALIGN_CENTER,Element.ALIGN_BASELINE,0,Rectangle.BOX,1,1)
    }
    chapter.add(table)
    
    chapter.add(Chunk.NEWLINE)
    chapter.add(Chunk.NEWLINE)
    chapter.add(Chunk.NEWLINE)

    table = new PdfPTable(7)
    table.setWidthPercentage(90)
    table.setWidths(31,21,10,21,5,7,5)
    table.setSpacingBefore(5)
    table.setSpacingAfter(5)
    
    cell = newCell(table,"", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.BOTTOM,1,1)
    cell = newCell(table,"", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.NO_BORDER,1,1)
    cell = newCell(table,"«___»", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.NO_BORDER,1,1)
    cell = newCell(table,"", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.BOTTOM,1,1)
    cell = newCell(table,"20", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.NO_BORDER,1,1)
    cell = newCell(table,"", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.BOTTOM,1,1)
   	cell = newCell(table,"г.", font, Element.ALIGN_LEFT,Element.ALIGN_BASELINE,0,Rectangle.NO_BORDER,1,1)
    chapter.add(table)

    //image example
    PdfPCell cell3 = new PdfPCell();
    cell3.setUseAscender(true)
    cell3.setFixedHeight((float)156)
    imgPathBack = "/data/jirasd/jira-server/imagecreation/ExampleImage.png"
    imgBack = Image.getInstance(imgPathBack)
    imgEvent = new ImageEvent(imgBack)
    cell3.setCellEvent(imgEvent)
    cell3.setBorder(Rectangle.NO_BORDER)
    table.addCell(cell3)
    
    String filePath = ComponentAccessor.getComponent(JiraHome).homePath + "/document.pdf"
    PdfWriter.getInstance(document, new FileOutputStream(filePath))
    document.open()
    document.add(chapter)
    document.close()
    createAttachment(issue,filePath,currentUser) 
}

PdfPCell newCell(PdfPTable table,String text, Font font, int horizontal, int vertical, int rotate, int borders, int colspan, int rowspan){
    PdfPCell cell = new PdfPCell()
    Paragraph par = new Paragraph(text, font)
    cell.setUseAscender(true)
    par.setAlignment(horizontal)
    cell.addElement(par)
    cell.setBorder(borders)
    cell.setColspan(colspan)
    cell.setRowspan(rowspan)
    cell.setMinimumHeight(15)
    cell.setNoWrap(false)
    cell.setVerticalAlignment(vertical)
    cell.setRotation(rotate)
    table.addCell(cell)
    return cell
}
    
Paragraph newCenteredParagraph(String text, Font font){
        Paragraph par=new Paragraph(text, font)
        par.setAlignment(par.ALIGN_CENTER)
        par.setLeading(10,0)
        return par
}

Paragraph newRightParagraph(String text, Font font){
        Paragraph par=new Paragraph(text, font)
        par.setAlignment(par.ALIGN_RIGHT)
        par.setLeading(15,0)
        return par
}

Paragraph newLeftParagraph(String text, Font font){
        Paragraph par=new Paragraph(text, font)
        par.setAlignment(par.ALIGN_LEFT)
        par.setLeading(5,0)
        return par
}
    
boolean createAttachment (MutableIssue issue, String tempFilePath, ApplicationUser currentUser){
    boolean isCreated = false
    File file = new File(tempFilePath)
    if (file.exists()){
        log.debug("Temp file ${tempFilePath} found")
        String fileName = file?.name

        String extension = FilenameUtils.getExtension(fileName)
            
        if (extension == ""){
            extension = "txt"
        }
        if (extension == "pdf"){
            extension = "application/pdf"
        }
        log.debug(extension)
        CreateAttachmentParamsBean bean = new CreateAttachmentParamsBean.Builder()
            .file(file)
            .filename(fileName)
            .contentType(extension)
            .author(currentUser)
            .issue(issue)
            .build()
        ComponentAccessor.attachmentManager.createAttachment(bean)
        isCreated = true
    }
    else{
        log.debug("Temp file ${tempFilePath} not found")
    }
    return isCreated
}

class ImageEvent implements PdfPCellEvent {
    protected Image img;
    public ImageEvent(Image img) {
        this.img = img;
    }
    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        img.scaleToFit(position.getWidth(), position.getHeight());
        float x = position.getLeft() + (position.getWidth() - img.getScaledWidth()) / 2
        float y = position.getBottom() + (position.getHeight() - img.getScaledHeight()) / 2
        img.setAbsolutePosition(x,y);
        PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
        try {
            canvas.addImage(img);
        } catch (DocumentException ex) {
            // do nothing
        }
    }
}