package com.erp.document.util;

import com.erp.document.entity.DocDocument;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 极简单证 PDF 生成器（B6 收尾）。
 *
 * <p>不引入重量级 PDF 库，直接按 PDF 1.4 语法手工拼一页纯文本单证，
 * 输出标题 + 关键字段（单证编号 / 类型 / 关联订单 / 标题 / 状态 / 备注）。
 * 满足「生成 PDF 后存 MinIO」的收尾需求；后续如需富文本排版再换 OpenPDF。
 */
public final class SimplePdfGenerator {

    private SimplePdfGenerator() {
    }

    /** 根据单证实体生成 PDF 字节流。 */
    public static byte[] generate(DocDocument doc) {
        List<String> lines = new ArrayList<>();
        lines.add("ERP DEMO2 - Trade Document");
        lines.add("================================");
        lines.add("Doc No   : " + nullToEmpty(doc.getDocNo()));
        lines.add("Doc Type : " + nullToEmpty(doc.getDocType()));
        lines.add("Title    : " + nullToEmpty(doc.getTitle()));
        lines.add("Order No : " + nullToEmpty(doc.getOrderNo()));
        lines.add("Template : " + nullToEmpty(doc.getTemplateCode()));
        lines.add("Status   : " + nullToEmpty(doc.getStatus()));
        lines.add("Remark   : " + nullToEmpty(doc.getRemark()));
        return buildSinglePagePdf(lines);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 用文本行拼一个单页 PDF（PDF 1.4，Helvetica 12pt，行距 18pt）。 */
    private static byte[] buildSinglePagePdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 12 Tf\n50 780 Td\n18 TL\n");
        for (int i = 0; i < lines.size(); i++) {
            String escaped = escapePdfText(lines.get(i));
            if (i == 0) {
                content.append("(").append(escaped).append(") Tj\n");
            } else {
                content.append("T*\n(").append(escaped).append(") Tj\n");
            }
        }
        content.append("ET");
        byte[] streamBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>");
        objects.add("<< /Length " + streamBytes.length + " >>\nstream\n" + content + "\nendstream");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder header = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        int runningOffset = header.toString().getBytes(StandardCharsets.ISO_8859_1).length;

        StringBuilder body = new StringBuilder();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(runningOffset + body.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            String obj = (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n";
            body.append(obj);
        }

        String bodyStr = body.toString();
        int xrefOffset = runningOffset + bodyStr.getBytes(StandardCharsets.ISO_8859_1).length;

        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objects.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (Integer off : offsets) {
            xref.append(String.format("%010d 00000 n \n", off));
        }
        xref.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xrefOffset).append("\n%%EOF");

        try {
            out.write(header.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.write(bodyStr.getBytes(StandardCharsets.ISO_8859_1));
            out.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private static String escapePdfText(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
