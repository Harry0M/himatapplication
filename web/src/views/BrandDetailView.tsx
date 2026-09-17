import React, { useState, useMemo } from "react"
import {
  ArrowLeft,
  Search,
  Tag,
  Package,
  Receipt,
  Building2,
  Edit2,
  Calendar,
  ExternalLink,
  Phone,
  MapPin,
  CheckCircle2,
  FileText,
  Layers,
  Sparkles,
  Info
} from "lucide-react"
import { useData } from "../context/DataContext"
import { formatInr, formatDate, cn } from "../lib/utils"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Input } from "../components/ui/Input"
import { ImageLightboxModal } from "../components/ui/ImageLightboxModal"
import { Brand } from "../types"

interface BrandDetailViewProps {
  brandId: number
  onBack: () => void
  onEdit?: (brand: Brand) => void
  onNavigate?: (tab: string) => void
}

export function BrandDetailView({
  brandId,
  onBack,
  onEdit,
  onNavigate,
}: BrandDetailViewProps) {
  const { brands, suppliers, products, entries, customers, visits } = useData()

  const [activeTab, setActiveTab] = useState<"products" | "orders" | "manufacturer">("products")
  const [searchQuery, setSearchQuery] = useState<string>("")
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // Target Brand
  const brand = useMemo(() => {
    return brands.find((b) => b.id === brandId) || null
  }, [brands, brandId])

  // Linked Manufacturer / Supplier
  const linkedSupplier = useMemo(() => {
    if (!brand) return null
    if (brand.manufacturerId) {
      return suppliers.find((s) => s.id === brand.manufacturerId) || null
    }
    if (brand.manufacturerName) {
      const mfg = brand.manufacturerName.toLowerCase()
      return (
        suppliers.find(
          (s) =>
            (s.firmName && s.firmName.toLowerCase() === mfg) ||
            (s.name && s.name.toLowerCase() === mfg)
        ) || null
      )
    }
    return null
  }, [brand, suppliers])

  // Products under this brand
  const brandProducts = useMemo(() => {
    if (!brand) return []
    const bName = brand.brandName.toLowerCase()
    return products.filter((p) => {
      const pName = (p.name || "").toLowerCase()
      const pCode = (p.productCode || "").toLowerCase()
      const pDesc = (p.description || "").toLowerCase()
      const pSupplierId = p.supplierId ? Number(p.supplierId) : null
      const matchesSupplier =
        brand.manufacturerId && pSupplierId === Number(brand.manufacturerId)
      const matchesText =
        pName.includes(bName) || pCode.includes(bName) || pDesc.includes(bName)
      return matchesSupplier || matchesText
    })
  }, [products, brand])

  // Orders containing products of this brand
  const brandOrders = useMemo(() => {
    if (!brand) return []
    const bName = brand.brandName.toLowerCase()
    const productCodes = new Set(brandProducts.map((p) => p.productCode.toLowerCase()))

    return entries
      .filter((e) => {
        const itemCode = (e.itemCode || "").toLowerCase()
        const supplierMatches =
          brand.manufacturerId && Number(e.supplierId) === Number(brand.manufacturerId)
        const codeMatches = productCodes.has(itemCode) || itemCode.includes(bName)
        return codeMatches || supplierMatches
      })
      .sort((a, b) => (b.id || 0) - (a.id || 0))
  }, [entries, brand, brandProducts])

  // Visits map for orders
  const visitMap = useMemo(() => {
    const map = new Map<number, (typeof visits)[0]>()
    visits.forEach((v) => map.set(v.id, v))
    return map
  }, [visits])

  // Customer map for orders
  const customerMap = useMemo(() => {
    const map = new Map<number, string>()
    customers.forEach((c) => {
      map.set(c.id, c.firmName || c.name)
    })
    return map
  }, [customers])

  // KPI Calculations
  const totalPiecesVolume = useMemo(() => {
    return brandOrders.reduce((sum, o) => sum + (Number(o.pieces) || 0), 0)
  }, [brandOrders])

  const totalInvoicedValue = useMemo(() => {
    return brandOrders.reduce(
      (sum, o) => sum + (Number(o.totalAmount) || 0) + (Number(o.gstAmount) || 0),
      0
    )
  }, [brandOrders])

  // Filtered Products
  const filteredProducts = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return brandProducts
    return brandProducts.filter(
      (p) =>
        (p.productCode || "").toLowerCase().includes(q) ||
        (p.name || "").toLowerCase().includes(q) ||
        (p.category || "").toLowerCase().includes(q) ||
        (p.hsnCode || "").toLowerCase().includes(q) ||
        (p.description || "").toLowerCase().includes(q)
    )
  }, [brandProducts, searchQuery])

  // Filtered Orders
  const filteredOrders = useMemo(() => {
    const q = searchQuery.toLowerCase().trim()
    if (!q) return brandOrders
    return brandOrders.filter((o) => {
      const visit = visitMap.get(o.visitId)
      const custName = (visit?.customerName || customerMap.get(visit?.customerId || 0) || "").toLowerCase()
      return (
        (o.itemCode || "").toLowerCase().includes(q) ||
        (o.orderNo || String(o.id) || "").toLowerCase().includes(q) ||
        custName.includes(q) ||
        (o.deliveryStatus || "").toLowerCase().includes(q) ||
        (o.paymentStatus || "").toLowerCase().includes(q)
      )
    })
  }, [brandOrders, searchQuery, visitMap, customerMap])

  if (!brand) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-2 text-xs">
          <ArrowLeft className="h-4 w-4" /> Back to Brands Master
        </Button>
        <Card className="p-8 text-center text-muted-foreground">
          Brand record not found or has been removed.
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={onBack}
            className="h-9 w-9 p-0 rounded-lg shrink-0"
          >
            <ArrowLeft className="h-4 w-4 text-zinc-600 dark:text-zinc-400" />
          </Button>

          <div className="flex items-center gap-3">
            {brand.logoPhotoUri ? (
              <img
                src={brand.logoPhotoUri}
                alt={brand.brandName}
                onClick={() =>
                  setLightbox({
                    open: true,
                    url: brand.logoPhotoUri || "",
                    title: `${brand.brandName} Official Brand Logo`,
                  })
                }
                className="h-11 w-11 rounded-lg border border-zinc-200 object-cover cursor-pointer hover:opacity-90 shadow-sm dark:border-zinc-800"
              />
            ) : (
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-amber-500/10 text-amber-700 font-bold dark:bg-amber-500/20 dark:text-amber-400">
                <Tag className="h-5 w-5" />
              </div>
            )}

            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
                  {brand.brandName}
                </h2>
                <Badge
                  variant="outline"
                  className={cn(
                    "text-[10px] font-semibold",
                    brand.isActive !== false
                      ? "bg-emerald-500/10 text-emerald-700 border-emerald-500/20"
                      : "bg-zinc-100 text-zinc-600 border-zinc-200"
                  )}
                >
                  {brand.isActive !== false ? "Active Brand" : "Inactive"}
                </Badge>
                <Badge
                  variant="outline"
                  className="text-[10px] bg-amber-500/10 text-amber-700 border-amber-500/20"
                >
                  {brand.category || "Apparel"}
                </Badge>
              </div>
              <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-0.5">
                {brand.manufacturerName
                  ? `Manufactured by ${brand.manufacturerName}`
                  : "Direct / Independent Label"}
                {brand.description && ` • "${brand.description}"`}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {onEdit && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onEdit(brand)}
              className="h-8 gap-1.5 text-xs"
            >
              <Edit2 className="h-3.5 w-3.5" />
              Edit Brand
            </Button>
          )}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Products Catalog</span>
            <Package className="h-4 w-4 text-blue-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {brandProducts.length}
          </div>
          <span className="text-[10px] text-zinc-400">Items tagged to brand</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Orders Generated</span>
            <Receipt className="h-4 w-4 text-emerald-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {brandOrders.length}
          </div>
          <span className="text-[10px] text-zinc-400">Total client bookings</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Volume Dispatched</span>
            <Layers className="h-4 w-4 text-amber-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {totalPiecesVolume.toLocaleString("en-IN")} pcs
          </div>
          <span className="text-[10px] text-zinc-400">Total pieces volume</span>
        </Card>

        <Card className="p-3.5 border border-zinc-200/80 dark:border-zinc-800">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-medium text-zinc-500">Total Sales Value</span>
            <Sparkles className="h-4 w-4 text-purple-500" />
          </div>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-50 mt-1">
            {formatInr(totalInvoicedValue)}
          </div>
          <span className="text-[10px] text-zinc-400">Invoiced gross revenue</span>
        </Card>
      </div>

      {/* Tabs & Search Navigation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-zinc-200 pb-3 dark:border-zinc-800">
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <button
            onClick={() => setActiveTab("products")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "products"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Package className="h-3.5 w-3.5" />
            Products Catalog ({brandProducts.length})
          </button>

          <button
            onClick={() => setActiveTab("orders")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "orders"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Receipt className="h-3.5 w-3.5" />
            Orders & Bookings ({brandOrders.length})
          </button>

          <button
            onClick={() => setActiveTab("manufacturer")}
            className={cn(
              "px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1.5",
              activeTab === "manufacturer"
                ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900 shadow-sm"
                : "text-zinc-600 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:bg-zinc-800"
            )}
          >
            <Building2 className="h-3.5 w-3.5" />
            Linked Mill / Supplier
          </button>
        </div>

        {/* Live Search */}
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
          <Input
            type="text"
            placeholder={
              activeTab === "products"
                ? "Search products, code, category..."
                : activeTab === "orders"
                ? "Search order #, customer, status..."
                : "Search details..."
            }
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 h-8 text-xs"
          />
        </div>
      </div>

      {/* TAB 1: Products */}
      {activeTab === "products" && (
        <div>
          {filteredProducts.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Package className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No products match your search query." : "No products added under this brand yet."}
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 border-b border-zinc-200 text-zinc-600 dark:bg-zinc-900 dark:border-zinc-800 dark:text-zinc-400 font-semibold">
                  <tr>
                    <th className="py-2.5 px-3">Product Code</th>
                    <th className="py-2.5 px-3">Product Name</th>
                    <th className="py-2.5 px-3">Category</th>
                    <th className="py-2.5 px-3 text-right">Default Rate</th>
                    <th className="py-2.5 px-3 text-center">Case Size</th>
                    <th className="py-2.5 px-3">HSN</th>
                    <th className="py-2.5 px-3">Description</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                  {filteredProducts.map((p) => (
                    <tr key={p.id} className="hover:bg-zinc-50/60 dark:hover:bg-zinc-900/60 transition-colors">
                      <td className="py-2.5 px-3 font-mono font-bold text-zinc-900 dark:text-zinc-100">
                        {p.productCode}
                      </td>
                      <td className="py-2.5 px-3 font-medium text-zinc-800 dark:text-zinc-200">
                        {p.name}
                      </td>
                      <td className="py-2.5 px-3">
                        <Badge variant="outline" className="text-[10px] bg-zinc-100 text-zinc-700">
                          {p.category || "Apparel"}
                        </Badge>
                      </td>
                      <td className="py-2.5 px-3 text-right font-bold text-emerald-600 dark:text-emerald-400">
                        {p.defaultRate ? formatInr(p.defaultRate) : "—"}
                      </td>
                      <td className="py-2.5 px-3 text-center text-zinc-600 dark:text-zinc-400">
                        {p.defaultCaseSize || 24} pcs
                      </td>
                      <td className="py-2.5 px-3 font-mono text-zinc-500 text-[11px]">
                        {p.hsnCode || "6203"}
                      </td>
                      <td className="py-2.5 px-3 text-zinc-500 max-w-xs truncate">
                        {p.description || "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: Orders */}
      {activeTab === "orders" && (
        <div>
          {filteredOrders.length === 0 ? (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs">
              <Receipt className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              {searchQuery ? "No orders match your search query." : "No orders recorded for this brand's merchandise yet."}
            </Card>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-950">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 border-b border-zinc-200 text-zinc-600 dark:bg-zinc-900 dark:border-zinc-800 dark:text-zinc-400 font-semibold">
                  <tr>
                    <th className="py-2.5 px-3">Order #</th>
                    <th className="py-2.5 px-3">Item Code</th>
                    <th className="py-2.5 px-3">Customer / Buyer</th>
                    <th className="py-2.5 px-3">Pieces</th>
                    <th className="py-2.5 px-3 text-right">Rate</th>
                    <th className="py-2.5 px-3 text-right">Total Amount</th>
                    <th className="py-2.5 px-3">Delivery Status</th>
                    <th className="py-2.5 px-3">Payment</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                  {filteredOrders.map((o) => {
                    const visit = visitMap.get(o.visitId)
                    const custName = visit?.customerName || customerMap.get(visit?.customerId || 0) || "Customer"
                    return (
                      <tr key={o.id} className="hover:bg-zinc-50/60 dark:hover:bg-zinc-900/60 transition-colors">
                        <td className="py-2.5 px-3 font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                          #{o.orderNo || o.id}
                        </td>
                        <td className="py-2.5 px-3 font-medium text-zinc-800 dark:text-zinc-200">
                          {o.itemCode}
                        </td>
                        <td className="py-2.5 px-3 font-medium text-zinc-700 dark:text-zinc-300">
                          {custName}
                        </td>
                        <td className="py-2.5 px-3 font-bold text-zinc-900 dark:text-zinc-100">
                          {o.pieces} pcs
                        </td>
                        <td className="py-2.5 px-3 text-right text-zinc-600 dark:text-zinc-400">
                          ₹{o.rate}
                        </td>
                        <td className="py-2.5 px-3 text-right font-bold text-emerald-700 dark:text-emerald-400">
                          {formatInr((Number(o.totalAmount) || 0) + (Number(o.gstAmount) || 0))}
                        </td>
                        <td className="py-2.5 px-3">
                          <Badge
                            variant="outline"
                            className={cn(
                              "text-[10px] font-semibold",
                              o.deliveryStatus?.toLowerCase() === "delivered"
                                ? "bg-emerald-500/10 text-emerald-700 border-emerald-500/20"
                                : o.deliveryStatus?.toLowerCase() === "dispatched"
                                ? "bg-blue-500/10 text-blue-700 border-blue-500/20"
                                : "bg-amber-500/10 text-amber-700 border-amber-500/20"
                            )}
                          >
                            {o.deliveryStatus || "Pending"}
                          </Badge>
                        </td>
                        <td className="py-2.5 px-3">
                          <Badge
                            variant="outline"
                            className={cn(
                              "text-[10px] font-semibold",
                              o.paymentStatus?.toLowerCase() === "paid"
                                ? "bg-emerald-500/10 text-emerald-700 border-emerald-500/20"
                                : "bg-rose-500/10 text-rose-700 border-rose-500/20"
                            )}
                          >
                            {o.paymentStatus || "Unpaid"}
                          </Badge>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: Manufacturer / Linked Mill */}
      {activeTab === "manufacturer" && (
        <div>
          {linkedSupplier ? (
            <Card className="p-5 border border-zinc-200/80 dark:border-zinc-800 max-w-2xl space-y-4">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-500/10 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400">
                    <Building2 className="h-6 w-6" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-zinc-900 dark:text-zinc-50">
                      {linkedSupplier.firmName || linkedSupplier.name}
                    </h3>
                    <span className="text-xs text-zinc-500">
                      {linkedSupplier.type || "Manufacturer / Textile Mill"} • {linkedSupplier.city || "Ahmedabad"}
                    </span>
                  </div>
                </div>

                <Badge variant="outline" className="text-xs bg-amber-500/10 text-amber-700 border-amber-500/20 font-semibold">
                  Official Mill
                </Badge>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs border-t border-zinc-100 pt-3 dark:border-zinc-800">
                {linkedSupplier.contactPerson && (
                  <div>
                    <span className="text-zinc-400 text-[11px] block">Contact Person</span>
                    <span className="font-medium text-zinc-800 dark:text-zinc-200">{linkedSupplier.contactPerson}</span>
                  </div>
                )}
                {linkedSupplier.phone && (
                  <div>
                    <span className="text-zinc-400 text-[11px] block">Primary Phone</span>
                    <span className="font-medium text-zinc-800 dark:text-zinc-200">{linkedSupplier.phone}</span>
                  </div>
                )}
                {linkedSupplier.address && (
                  <div className="sm:col-span-2">
                    <span className="text-zinc-400 text-[11px] block">Mill / Factory Address</span>
                    <span className="font-medium text-zinc-800 dark:text-zinc-200">{linkedSupplier.address}</span>
                  </div>
                )}
                {linkedSupplier.gstin && (
                  <div>
                    <span className="text-zinc-400 text-[11px] block">GSTIN</span>
                    <span className="font-mono text-zinc-800 dark:text-zinc-200">{linkedSupplier.gstin}</span>
                  </div>
                )}
                {linkedSupplier.marketName && (
                  <div>
                    <span className="text-zinc-400 text-[11px] block">Market Cluster</span>
                    <span className="font-medium text-zinc-800 dark:text-zinc-200">{linkedSupplier.marketName}</span>
                  </div>
                )}
              </div>
            </Card>
          ) : (
            <Card className="p-8 text-center text-zinc-500 border-dashed text-xs max-w-xl">
              <Building2 className="h-8 w-8 mx-auto text-zinc-300 mb-2" />
              <p className="font-semibold text-zinc-700 dark:text-zinc-300">No Manufacturer Link Found</p>
              <p className="text-zinc-400 mt-1">
                {brand.manufacturerName
                  ? `Brand specifies manufacturer "${brand.manufacturerName}", but no matching profile is registered in Suppliers master.`
                  : "This brand is registered as an independent label without a linked mill."}
              </p>
            </Card>
          )}
        </div>
      )}

      {/* Lightbox for Brand Logo */}
      <ImageLightboxModal
        open={lightbox.open}
        onClose={() => setLightbox({ open: false, url: "", title: "" })}
        imageUrl={lightbox.url}
        title={lightbox.title}
      />
    </div>
  )
}
