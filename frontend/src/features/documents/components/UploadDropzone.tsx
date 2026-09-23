import { useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { UploadCloud, FileText } from "lucide-react";

interface UploadDropzoneProps {
  onUpload: (files: File[]) => void;
  isUploading?: boolean;
}

export function UploadDropzone({
  onUpload,
  isUploading = false,
}: Readonly<UploadDropzoneProps>) {
  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (acceptedFiles.length > 0 && !isUploading) {
        onUpload(acceptedFiles);
      }
    },
    [onUpload, isUploading],
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
    maxSize: 50 * 1024 * 1024, // 50 MB
    disabled: isUploading,
  });

  return (
    <div
      {...getRootProps()}
      className={`border-2 border-dashed rounded-xl p-8 text-center transition-colors cursor-pointer ${
        isDragActive
          ? "border-primary-500 bg-primary-50"
          : "border-slate-300 hover:border-primary-400 hover:bg-slate-50"
      } ${isUploading ? "opacity-50 cursor-not-allowed" : ""}`}
    >
      <input {...getInputProps()} />

      <div className="flex flex-col items-center gap-3">
        <div
          className={`h-12 w-12 rounded-full flex items-center justify-center ${
            isDragActive ? "bg-primary-100" : "bg-slate-100"
          }`}
        >
          {isDragActive ? (
            <FileText className="h-6 w-6 text-primary-600" />
          ) : (
            <UploadCloud className="h-6 w-6 text-slate-400" />
          )}
        </div>

        <div>
          <p className="text-sm font-medium text-slate-900">
            {isDragActive ? "Drop files here" : "Upload documents"}
          </p>
          <p className="text-sm text-slate-500 mt-1">
            Drag & drop or click to browse
          </p>
        </div>

        <p className="text-xs text-slate-400">
          PDF, DOCX, XLSX, PNG, JPG up to 50 MB
        </p>
      </div>
    </div>
  );
}
