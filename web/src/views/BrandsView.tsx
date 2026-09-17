import React, { useState, useEffect } from "react"
import {
  Tag,
  Plus,
  Search,
  Building2,
  Edit2,
  Trash2,
  Check,
  X,
  Sparkles,
  Info
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { FileUpload } from "../components/ui/FileUpload"
import { ImageLightboxModal } from "../components/ui/ImageLightboxModal"
import { Brand } from "../types"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { getMasterDraft, saveMasterDraft, clearMasterDraft } from "../lib/masterDrafts"

export function BrandsView() {
  const { brands, suppliers, saveBrand, deleteBrand } = useData()
  const [search, setSearch] = useState("")
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingBrand, setEditingBrand] = useState<Brand | null>(null)
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // Form State
  const [brandName, setBrandName] = useState("")
  const [manufacturerId, setManufacturerId] = useState<number | "">("")
  const [category, setCategory] = useState("Denim & Jeans")
  const [description, setDescription] = useState("")
  const [logoPhotoUri, setLogoPhotoUri] = useState("")
  const [isActive, setIsActive] = useState(true)
  const [hasDraft, setHasDraft] = useState(false)

  const openAddModal = () => {
    setEditingBrand(null)
    const draft = getMasterDraft<any>("brand")
    if (draft) {
      setBrandName(draft.brandName || "")
      setManufacturerId(draft.manufacturerId || "")
      setCategory(draft.category || GARMENT_CATEGORIES[0] || "Denim & Jeans")
      setDescription(draft.description || "")
      setLogoPhotoUri("")
      setIsActive(true)
      setHasDraft(true)
    } else {
      setBrandName("")
      setManufacturerId("")
      setCategory(GARMENT_CATEGORIES[0] || "Denim & Jeans")
      setDescription("")
      setLogoPhotoUri("")
      setIsActive(true)
      setHasDraft(false)
    }
    setIsModalOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("brand")
    setHasDraft(false)
    setBrandName("")
    setManufacturerId("")
    setCategory(GARMENT_CATEGORIES[0] || "Denim & Jeans")
    setDescription("")
    setLogoPhotoUri("")
    setIsActive(true)
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isModalOpen || editingBrand !== null) return
    if (brandName.trim() || description.trim()) {
      saveMasterDraft("brand", {
        brandName,
        manufacturerId,
        category,
        description,
      })
    }
  }, [isModalOpen, editingBrand, brandName, manufacturerId, category, description])

  const openEditModal = (b: Brand) => {
    setEditingBrand(b)
    setBrandName(b.brandName || "")
    setManufacturerId(b.manufacturerId || "")
    setCategory(b.category || GARMENT_CATEGORIES[0] || "Denim & Jeans")
    setDescription(b.description || "")
    setLogoPhotoUri(b.logoPhotoUri || "")
    setIsActive(b.isActive ?? true)
    setHasDraft(false)
    setIsModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!brandName.trim()) return

    const selectedSupplier = suppliers.find((s) => s.id === Number(manufacturerId))

    const brandPayload: Brand = {
      id: editingBrand?.id || Date.now(),
      brandName: brandName.trim(),
      manufacturerId: manufacturerId ? Number(manufacturerId) : null,
      manufacturerName: selectedSupplier?.firmName || selectedSupplier?.name || "",
      category,
      logoPhotoUri,
      description: description.trim(),
      isActive,
      createdAt: editingBrand?.createdAt || Date.now(),
    }

    await saveBrand(brandPayload)
    clearMasterDraft("brand")
    setHasDraft(false)
    setIsModalOpen(false)
  }


  const handleDelete = async (id: number) => {
    if (confirm("Are you sure you want to delete this brand?")) {
      await deleteBrand(id)
    }
  }

  const filteredBrands = brands.filter((b) => {
    const q = search.toLowerCase()
    const name = (b.brandName || (b as any).name || "").toLowerCase()
    const mfg = (b.manufacturerName || "").toLowerCase()
    const cat = (b.category || "").toLowerCase()
    return name.includes(q) || mfg.includes(q) || cat.includes(q)
  })

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              Brand Master
            </h2>
            <Badge variant="outline" className="text-xs bg-amber-500/10 text-amber-700 border-amber-500/20 font-semibold">
              {brands.length} Brands
            </Badge>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Standard garment labels, mills, and proprietary manufacturer brands
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <Input
              type="text"
              placeholder="Search brands or mills..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 h-9 text-xs"
            />
          </div>
          <Button onClick={openAddModal} className="h-9 gap-1.5 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
            <Plus className="h-3.5 w-3.5" />
            Add Brand
          </Button>
        </div>
      </div>

      {/* Grid of Brands */}
      {filteredBrands.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12 text-center border-dashed">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-500 mb-3">
            <Tag className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No brands found</h3>
          <p className="text-xs text-zinc-500 max-w-sm mt-1">
            {search ? "No brands match your search query." : "Start building your brand master by registering manufacturer and wholesale labels."}
          </p>
          <Button onClick={openAddModal} variant="outline" className="mt-4 h-8 text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Register First Brand
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredBrands.map((brand) => (
            <Card key={brand.id} className="group relative overflow-hidden border border-zinc-200/80 p-4 transition-all hover:shadow-md dark:border-zinc-800">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div
                    onClick={() => {
                      if (brand.logoPhotoUri) {
                        setLightbox({
                          open: true,
                          url: brand.logoPhotoUri,
                          title: `${brand.brandName || (brand as any).name || "Brand"} Logo`,
                        })
                      }
                    }}
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-700 font-bold text-base dark:bg-amber-500/20 dark:text-amber-400 overflow-hidden border border-amber-200/50 dark:border-amber-800/50 ${
                      brand.logoPhotoUri ? "cursor-pointer hover:ring-2 hover:ring-amber-500/50 hover:scale-105 transition-transform" : ""
                    }`}
                    title={brand.logoPhotoUri ? "Click to view full screen & download" : undefined}
                  >
                    {brand.logoPhotoUri ? (
                      <img
                        src={brand.logoPhotoUri}
                        alt={brand.brandName}
                        className="h-full w-full object-cover"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = "none"
                        }}
                      />
                    ) : (
                      (brand.brandName || (brand as any).name) ? (brand.brandName || (brand as any).name)[0].toUpperCase() : "B"
                    )}
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-50 line-clamp-1">
                      {brand.brandName || (brand as any).name || "Brand"}
                    </h4>
                    <span className="inline-block text-[11px] font-medium text-amber-700 dark:text-amber-400">
                      {brand.category || "Apparel"}
                    </span>
                  </div>
                </div>


                <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => openEditModal(brand)}
                    className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDelete(brand.id)}
                    className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>

              <div className="mt-3 space-y-1.5 border-t border-zinc-100 pt-3 dark:border-zinc-800 text-xs">
                <div className="flex items-center gap-1.5 text-zinc-600 dark:text-zinc-400">
                  <Building2 className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                  <span className="truncate">
                    {brand.manufacturerName ? (
                      <span className="font-medium text-zinc-800 dark:text-zinc-200">{brand.manufacturerName}</span>
                    ) : (
                      <span className="italic text-zinc-400">Direct / Independent</span>
                    )}
                  </span>
                </div>
                {brand.description && (
                  <p className="text-[11px] text-zinc-500 line-clamp-2 italic pt-1">
                    "{brand.description}"
                  </p>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingBrand ? "Edit Brand" : "Add Brand Master"}
      >
        <div className="space-y-4 pt-1 max-h-[80vh] overflow-y-auto pr-1">
          {/* Draft Notification Banner */}
          {hasDraft && !editingBrand && (
            <div className="flex items-center justify-between rounded-lg border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs text-emerald-800">
              <div className="flex items-center gap-2">
                <Info className="h-4 w-4 text-emerald-600 shrink-0" />
                <span className="font-medium">Resumed from your local draft</span>
              </div>
              <button
                type="button"
                onClick={handleDiscardDraft}
                className="font-semibold text-rose-600 hover:text-rose-700 hover:underline cursor-pointer"
              >
                Discard Draft
              </button>
            </div>
          )}

          <form onSubmit={handleSave} className="space-y-4 text-xs">
          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Brand Name *
            </label>
            <Input
              required
              placeholder="e.g. V-Denim, Royal Cotton, Sparkle Kids"
              value={brandName}
              onChange={(e) => setBrandName(e.target.value)}
              className="h-9 text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Linked Manufacturer / Mill (Optional)
            </label>
            <select
              value={manufacturerId}
              onChange={(e) => setManufacturerId(e.target.value ? Number(e.target.value) : "")}
              className="w-full h-9 rounded-md border border-zinc-300 bg-white px-3 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            >
              <option value="">-- Direct Brand / Unlinked --</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firmName || s.name} ({s.type || "Supplier"})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Garment Category *
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full h-9 rounded-md border border-zinc-300 bg-white px-3 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            >
              {GARMENT_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>

          <FileUpload
            label="Brand Logo / Monogram Photo"
            folder={`brands/${(brandName || "unnamed").trim().replace(/\s+/g, "_")}`}
            prefix="logo"
            value={logoPhotoUri}
            onChange={setLogoPhotoUri}
          />

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Brand Notes & Description
            </label>
            <textarea
              rows={2}
              placeholder="e.g. Premium knitted polo shirts, 100% combed cotton, 220 GSM"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md border border-zinc-300 bg-white p-2 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            />
          </div>


          <div className="flex justify-end gap-2 pt-2 border-t border-zinc-200 dark:border-zinc-800">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)} className="h-8 text-xs">
              Cancel
            </Button>
            <Button type="submit" className="h-8 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
              {editingBrand ? "Save Changes" : "Create Brand"}
            </Button>
          </div>
        </form>
        </div>
      </Dialog>

      {/* Fullscreen Lightbox Modal with Download & Zoom */}
      <ImageLightboxModal
        open={lightbox.open}
        onClose={() => setLightbox({ open: false, url: "", title: "" })}
        imageUrl={lightbox.url}
        title={lightbox.title}
      />
    </div>
  )
}
