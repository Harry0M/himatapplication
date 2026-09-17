import React, { useState, useEffect } from "react"
import {
  Package,
  Plus,
  Search,
  Building2,
  Edit2,
  Trash2,
  Check,
  X,
  IndianRupee,
  Boxes,
  FileCode,
  Tag,
  Info
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Product } from "../types"
import { GARMENT_CATEGORIES } from "../lib/constants"
import { formatInr } from "../lib/utils"
import { getMasterDraft, saveMasterDraft, clearMasterDraft } from "../lib/masterDrafts"

export function ProductsView() {
  const { products, suppliers, saveProduct, deleteProduct } = useData()
  const [search, setSearch] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("all")
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState<Product | null>(null)
  const [hasDraft, setHasDraft] = useState(false)

  // Form State
  const [productCode, setProductCode] = useState("")
  const [name, setName] = useState("")
  const [supplierId, setSupplierId] = useState<number | "">("")
  const [category, setCategory] = useState(GARMENT_CATEGORIES[0] || "Denim & Jeans")
  const [defaultRate, setDefaultRate] = useState("350")
  const [defaultCaseSize, setDefaultCaseSize] = useState("24")
  const [hsnCode, setHsnCode] = useState("6203")
  const [description, setDescription] = useState("")

  const openAddModal = () => {
    setEditingProduct(null)
    const draft = getMasterDraft<any>("product")
    if (draft) {
      setProductCode(draft.productCode || "")
      setName(draft.name || "")
      setSupplierId(draft.supplierId || "")
      setCategory(draft.category || GARMENT_CATEGORIES[0] || "Denim & Jeans")
      setDefaultRate(draft.defaultRate !== undefined ? String(draft.defaultRate) : "350")
      setDefaultCaseSize(draft.defaultCaseSize !== undefined ? String(draft.defaultCaseSize) : "24")
      setHsnCode(draft.hsnCode || "6203")
      setDescription(draft.description || "")
      setHasDraft(true)
    } else {
      setProductCode(`PRD-${Math.floor(100 + Math.random() * 900)}`)
      setName("")
      setSupplierId(suppliers[0]?.id || "")
      setCategory(GARMENT_CATEGORIES[0] || "Denim & Jeans")
      setDefaultRate("350")
      setDefaultCaseSize("24")
      setHsnCode("6203")
      setDescription("")
      setHasDraft(false)
    }
    setIsModalOpen(true)
  }

  const openEditModal = (p: Product) => {
    setEditingProduct(p)
    setProductCode(p.productCode || "")
    setName(p.name || "")
    setSupplierId(p.supplierId || "")
    setCategory(p.category || GARMENT_CATEGORIES[0] || "Denim & Jeans")
    setDefaultRate(p.defaultRate !== undefined ? String(p.defaultRate) : "350")
    setDefaultCaseSize(p.defaultCaseSize !== undefined ? String(p.defaultCaseSize) : "24")
    setHsnCode(p.hsnCode || "6203")
    setDescription(p.description || "")
    setHasDraft(false)
    setIsModalOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("product")
    setHasDraft(false)
    setProductCode(`PRD-${Math.floor(100 + Math.random() * 900)}`)
    setName("")
    setSupplierId(suppliers[0]?.id || "")
    setCategory(GARMENT_CATEGORIES[0] || "Denim & Jeans")
    setDefaultRate("350")
    setDefaultCaseSize("24")
    setHsnCode("6203")
    setDescription("")
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isModalOpen || editingProduct !== null) return
    if (productCode.trim() || name.trim() || description.trim()) {
      saveMasterDraft("product", {
        productCode,
        name,
        supplierId,
        category,
        defaultRate,
        defaultCaseSize,
        hsnCode,
        description,
      })
    }
  }, [
    isModalOpen,
    editingProduct,
    productCode,
    name,
    supplierId,
    category,
    defaultRate,
    defaultCaseSize,
    hsnCode,
    description,
  ])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!productCode.trim() || !name.trim()) return

    const selectedSupplier = suppliers.find((s) => s.id === Number(supplierId))

    const productPayload: Product = {
      id: editingProduct?.id || Date.now(),
      productCode: productCode.trim().toUpperCase(),
      name: name.trim(),
      supplierId: selectedSupplier ? selectedSupplier.id : 0,
      supplierName: selectedSupplier ? (selectedSupplier.firmName || selectedSupplier.name) : "",
      category,
      defaultRate: parseFloat(defaultRate) || 0,
      defaultCaseSize: parseInt(defaultCaseSize, 10) || 24,
      hsnCode: hsnCode.trim(),
      description: description.trim(),
    }

    await saveProduct(productPayload)
    clearMasterDraft("product")
    setHasDraft(false)
    setIsModalOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (confirm("Are you sure you want to remove this product?")) {
      await deleteProduct(id)
    }
  }

  const filteredProducts = products.filter((p) => {
    const q = search.toLowerCase()
    const matchesQuery =
      (p.productCode || "").toLowerCase().includes(q) ||
      (p.name || "").toLowerCase().includes(q) ||
      (p.supplierName || "").toLowerCase().includes(q) ||
      (p.category || "").toLowerCase().includes(q) ||
      (p.hsnCode || "").toLowerCase().includes(q)

    const matchesCategory =
      categoryFilter === "all" || (p.category || "").toLowerCase() === categoryFilter.toLowerCase()

    return matchesQuery && matchesCategory
  })

  // Summary Metrics
  const totalCatalogValue = products.reduce((sum, p) => sum + (p.defaultRate || 0), 0)
  const averageRate = products.length > 0 ? Math.round(totalCatalogValue / products.length) : 0
  const uniqueSuppliersWithProducts = new Set(products.map((p) => p.supplierId).filter(Boolean)).size

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              Product Master
            </h2>
            <Badge
              variant="outline"
              className="text-xs bg-indigo-500/10 text-indigo-700 border-indigo-500/20 font-semibold"
            >
              {products.length} Products
            </Badge>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Configure garment product codes, mill mappings, case sizes, standard rates & HSN codes
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <Input
              type="text"
              placeholder="Search code, name, supplier..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 h-9 text-xs"
            />
          </div>
          <Button
            onClick={openAddModal}
            className="h-9 gap-1.5 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900"
          >
            <Plus className="h-3.5 w-3.5" />
            Add Product
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="p-4 rounded-2xl border border-zinc-200/80 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">Registered Products</p>
            <h3 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">{products.length}</h3>
            <p className="text-[11px] text-zinc-400 mt-0.5">Active codes in catalog</p>
          </div>
          <div className="h-11 w-11 rounded-2xl bg-indigo-500/10 text-indigo-600 dark:bg-indigo-500/20 dark:text-indigo-400 flex items-center justify-center">
            <Package className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 rounded-2xl border border-zinc-200/80 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">Mapped Suppliers / Mills</p>
            <h3 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
              {uniqueSuppliersWithProducts}
            </h3>
            <p className="text-[11px] text-zinc-400 mt-0.5">Manufacturers linked</p>
          </div>
          <div className="h-11 w-11 rounded-2xl bg-emerald-500/10 text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400 flex items-center justify-center">
            <Building2 className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 rounded-2xl border border-zinc-200/80 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">Average Product Rate</p>
            <h3 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
              ₹{formatInr(averageRate)}
            </h3>
            <p className="text-[11px] text-zinc-400 mt-0.5">Standard per-piece basis</p>
          </div>
          <div className="h-11 w-11 rounded-2xl bg-amber-500/10 text-amber-600 dark:bg-amber-500/20 dark:text-amber-400 flex items-center justify-center">
            <IndianRupee className="h-5 w-5" />
          </div>
        </Card>
      </div>

      {/* Category Pills Filter */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
        <button
          onClick={() => setCategoryFilter("all")}
          className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
            categoryFilter === "all"
              ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
              : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200 dark:bg-zinc-800 dark:text-zinc-300"
          }`}
        >
          All Categories ({products.length})
        </button>
        {GARMENT_CATEGORIES.map((cat) => {
          const count = products.filter((p) => (p.category || "").toLowerCase() === cat.toLowerCase()).length
          if (count === 0 && categoryFilter !== cat) return null
          return (
            <button
              key={cat}
              onClick={() => setCategoryFilter(cat)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
                categoryFilter === cat
                  ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                  : "bg-zinc-100 text-zinc-600 hover:bg-zinc-200 dark:bg-zinc-800 dark:text-zinc-300"
              }`}
            >
              {cat} ({count})
            </button>
          )
        })}
      </div>

      {/* Grid of Products */}
      {filteredProducts.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12 text-center border-dashed">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-500 mb-3">
            <Package className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No products found</h3>
          <p className="text-xs text-zinc-500 max-w-sm mt-1">
            {search
              ? "No products match your search query."
              : "Register your garment item codes, supplier associations and case sizes."}
          </p>
          <Button onClick={openAddModal} variant="outline" className="mt-4 h-8 text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Register First Product
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filteredProducts.map((prod) => (
            <Card
              key={prod.id}
              className="group relative overflow-hidden border border-zinc-200/80 p-4 transition-all hover:shadow-md dark:border-zinc-800 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <span className="font-mono text-[11px] font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded-md">
                      {prod.productCode}
                    </span>
                    <h4 className="font-bold text-sm text-zinc-900 dark:text-zinc-100 truncate mt-1.5">
                      {prod.name}
                    </h4>
                  </div>
                  <Badge variant="secondary" className="text-[10px] shrink-0 font-medium">
                    {prod.category}
                  </Badge>
                </div>

                <div className="mt-3 space-y-1.5 text-xs text-zinc-600 dark:text-zinc-300">
                  <div className="flex items-center gap-1.5 truncate">
                    <Building2 className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                    <span className="truncate font-medium text-zinc-800 dark:text-zinc-200">
                      {prod.supplierName || "Direct Mill"}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-zinc-100 dark:border-zinc-800 text-[11px]">
                    <div>
                      <span className="text-muted-foreground">Rate:</span>
                      <p className="font-bold text-zinc-900 dark:text-zinc-100">
                        ₹{formatInr(prod.defaultRate || 0)}
                      </p>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Case Size:</span>
                      <p className="font-bold text-zinc-900 dark:text-zinc-100">
                        {prod.defaultCaseSize || 24} pcs
                      </p>
                    </div>
                  </div>

                  {prod.hsnCode && (
                    <div className="flex items-center gap-1 text-[11px] text-muted-foreground pt-1">
                      <FileCode className="h-3 w-3 text-zinc-400 shrink-0" />
                      <span>HSN: {prod.hsnCode}</span>
                    </div>
                  )}

                  {prod.description && (
                    <p className="text-[11px] text-zinc-500 dark:text-zinc-400 line-clamp-2 pt-1 italic">
                      "{prod.description}"
                    </p>
                  )}
                </div>
              </div>

              {/* Action buttons */}
              <div className="mt-4 flex items-center justify-end gap-1 pt-2 border-t border-zinc-100 dark:border-zinc-800">
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => openEditModal(prod)}
                  className="h-7 w-7 p-0 text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
                  title="Edit Product"
                >
                  <Edit2 className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => handleDelete(prod.id)}
                  className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                  title="Delete Product"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Add / Edit Product Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingProduct ? "Edit Product" : "Register Product Master"}
        description="Standardized garment item code, manufacturer mapping, default rates, and case pack specifications"
      >
        <div className="space-y-4 pt-1 max-h-[80vh] overflow-y-auto pr-1">
          {/* Draft Notification Banner */}
          {hasDraft && !editingProduct && (
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
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Product Code <span className="text-red-500">*</span>
                </label>
                <Input
                  required
                  placeholder="e.g. D-101, SH-204"
                  value={productCode}
                  onChange={(e) => setProductCode(e.target.value)}
                  className="h-9 text-xs font-mono uppercase"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Product Name / Title <span className="text-red-500">*</span>
                </label>
                <Input
                  required
                  placeholder="e.g. Regular Fit Cotton Jeans"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="h-9 text-xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Linked Supplier / Manufacturer <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value ? Number(e.target.value) : "")}
                  className="w-full h-9 rounded-md border border-zinc-300 bg-white px-3 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
                >
                  <option value="">-- Select Supplier / Mill --</option>
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.firmName || s.name} ({s.city || "Ahmedabad"})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Garment Category <span className="text-red-500">*</span>
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
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Default Rate (₹ / pc)
                </label>
                <Input
                  type="number"
                  placeholder="350"
                  value={defaultRate}
                  onChange={(e) => setDefaultRate(e.target.value)}
                  className="h-9 text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  Default Case Size (pcs)
                </label>
                <Input
                  type="number"
                  placeholder="24"
                  value={defaultCaseSize}
                  onChange={(e) => setDefaultCaseSize(e.target.value)}
                  className="h-9 text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                  HSN Code
                </label>
                <Input
                  placeholder="6203"
                  value={hsnCode}
                  onChange={(e) => setHsnCode(e.target.value)}
                  className="h-9 text-xs font-mono"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Fabric & Product Description
              </label>
              <textarea
                rows={2}
                placeholder="e.g. 100% cotton ring denim, enzyme washed, 5-pocket styling"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-md border border-zinc-300 bg-white p-2.5 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-zinc-200 dark:border-zinc-800">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsModalOpen(false)}
                className="h-8 text-xs"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                className="h-8 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900"
              >
                {editingProduct ? "Save Changes" : "Create Product"}
              </Button>
            </div>
          </form>
        </div>
      </Dialog>
    </div>
  )
}
