import React, { useEffect, useRef } from "react"

interface AutoGrowTextareaProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  minRows?: number
  maxRows?: number
  className?: string
  id?: string
  name?: string
  required?: boolean
  maxLength?: number
  disabled?: boolean
}

/**
 * Auto-growing textarea: starts at minRows height and expands vertically
 * as the user types (works the same on phone and PC). Never shrinks below
 * the content height, so long text always stays fully visible.
 */
export const AutoGrowTextarea: React.FC<AutoGrowTextareaProps> = ({
  value,
  onChange,
  placeholder,
  minRows = 2,
  maxRows = 12,
  className = "",
  ...rest
}) => {
  const ref = useRef<HTMLTextAreaElement | null>(null)

  const resize = () => {
    const el = ref.current
    if (!el) return
    el.style.height = "auto"
    el.style.height = `${Math.min(el.scrollHeight, 100000)}px`
  }

  useEffect(() => {
    resize()
  }, [value])

  const lineHeight = 16 // approximate line height for text-xs (12px font, 1.33)
  const maxH = lineHeight * maxRows + 24 // padding allowance

  return (
    <textarea
      ref={ref}
      rows={minRows}
      value={value}
      placeholder={placeholder}
      onChange={(e) => {
        onChange(e.target.value)
        // Resize on next frame after value update
        requestAnimationFrame(resize)
      }}
      style={{
        overflowY: "auto",
        maxHeight: `${maxH}px`,
        height: "auto",
      }}
      className={className}
      {...rest}
    />
  )
}

export default AutoGrowTextarea
