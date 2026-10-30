/** True when phones cannot open the link, because it points at the laptop itself. */
export function isLocalOnlyLink(url: string): boolean {
  const host = new URL(url).hostname
  return (
    host === 'localhost' ||
    host.endsWith('.localhost') ||
    host.startsWith('127.') ||
    host === '[::1]'
  )
}
