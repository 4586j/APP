package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import lombok.Data;

/** 路径解析结果。 */
@Data
public class ResolvedPath {
    public enum Type { ROOT, DEPT_ROOT, FOLDER, FILE, NOT_FOUND }

    private Type type;
    private Long deptId;          // DEPT_ROOT/FOLDER/FILE 时为所在部门
    private DatFile datFile;      // FOLDER/FILE 时非 null
    private String basePath;      // 当前路径的 href（用于 PROPFIND basePath）

    public static ResolvedPath root(String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.ROOT; r.basePath = basePath; return r;
    }
    public static ResolvedPath deptRoot(Long deptId, String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.DEPT_ROOT; r.deptId = deptId; r.basePath = basePath; return r;
    }
    public static ResolvedPath of(DatFile f, String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = f.getIsDirectory() != null && f.getIsDirectory() == 1 ? Type.FOLDER : Type.FILE;
        r.datFile = f; r.deptId = f.getDeptId(); r.basePath = basePath; return r;
    }
    public static ResolvedPath notFound(String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.NOT_FOUND; r.basePath = basePath; return r;
    }
}
