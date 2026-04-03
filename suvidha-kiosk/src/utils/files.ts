export async function fileToBase64(file: File) {
  const base64 = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result ?? ''))
    reader.onerror = () => reject(new Error('file_read_failed'))
    reader.readAsDataURL(file)
  })

  // API expects base64 bytes, not a data URL.
  const comma = base64.indexOf(',')
  return comma >= 0 ? base64.slice(comma + 1) : base64
}
