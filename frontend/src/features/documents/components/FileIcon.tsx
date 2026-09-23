import { FileText, FileImage, FileSpreadsheet, File } from "lucide-react";

interface FileIconProps {
  mimeType: string;
  className?: string;
}

export function FileIcon({ mimeType, className = "h-5 w-5" }: Readonly<FileIconProps>) {
  if (mimeType.includes("pdf")) {
    return <FileText className={`${className} text-red-500`} />;
  }
  if (mimeType.includes("image")) {
    return <FileImage className={`${className} text-green-500`} />;
  }
  if (mimeType.includes("word")) {
    return <FileText className={`${className} text-blue-500`} />;
  }
  if (mimeType.includes("excel") || mimeType.includes("sheet")) {
    return <FileSpreadsheet className={`${className} text-emerald-500`} />;
  }
  return <File className={`${className} text-slate-500`} />;
}
