/**
 * Maps a file name to the Lucide icon used in the file table / card list.
 *
 * The mapping is extension-driven (case-insensitive) and intentionally
 * coarse — covering the families called out in the issue DoD (PDF, MD,
 * archives, images, Office docs, code, audio/video) and falling back to
 * a generic file icon for everything else. Returning the icon component
 * rather than a JSX element keeps the call site free to set its own size
 * and colour.
 */
import {
  Archive,
  File as FileIcon,
  FileAudio,
  FileCode,
  FileImage,
  FileSpreadsheet,
  FileText,
  FileVideo,
  type LucideIcon,
} from "lucide-react-native";

const EXTENSION_ICON: Record<string, LucideIcon> = {
  pdf: FileText,
  md: FileText,
  txt: FileText,
  rtf: FileText,
  doc: FileText,
  docx: FileText,
  odt: FileText,
  xls: FileSpreadsheet,
  xlsx: FileSpreadsheet,
  csv: FileSpreadsheet,
  ods: FileSpreadsheet,
  zip: Archive,
  rar: Archive,
  "7z": Archive,
  tar: Archive,
  gz: Archive,
  png: FileImage,
  jpg: FileImage,
  jpeg: FileImage,
  gif: FileImage,
  webp: FileImage,
  svg: FileImage,
  bmp: FileImage,
  mp3: FileAudio,
  wav: FileAudio,
  ogg: FileAudio,
  flac: FileAudio,
  mp4: FileVideo,
  mov: FileVideo,
  webm: FileVideo,
  mkv: FileVideo,
  ts: FileCode,
  tsx: FileCode,
  js: FileCode,
  jsx: FileCode,
  java: FileCode,
  py: FileCode,
  go: FileCode,
  rs: FileCode,
  c: FileCode,
  h: FileCode,
  cpp: FileCode,
  json: FileCode,
  xml: FileCode,
  yaml: FileCode,
  yml: FileCode,
  html: FileCode,
  css: FileCode,
};

/**
 * Returns the file extension (lower-cased, no leading dot) of the given
 * name. Returns `''` when the name has no extension.
 */
function extensionOf(name: string): string {
  const dot = name.lastIndexOf(".");
  if (dot < 0 || dot === name.length - 1) return "";
  return name.slice(dot + 1).toLowerCase();
}

/**
 * Returns the Lucide icon matching the file's extension. Files without a
 * recognised extension get the generic {@link FileIcon}.
 *
 * @param name - File name including the extension (e.g. `Skript_Mathe.pdf`).
 * @returns The Lucide icon component to render.
 */
export function fileIconFor(name: string): LucideIcon {
  return EXTENSION_ICON[extensionOf(name)] ?? FileIcon;
}
