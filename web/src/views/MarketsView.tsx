import React, { useState, useEffect } from "react"
import {
  Compass,
  Plus,
  Search,
  MapPin,
  Building2,
  Edit2,
  Trash2,
  Sparkles
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Market } from "../types"
import { AHMEDABAD_TEXTILE_MARKETS } from "../lib/constants"

export function MarketsView() {
  const { markets, suppliers, saveMarket, deleteMarket } = useData()
  const [search, setSearch] = useState("")
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingMarket, setEditingMarket] = useState<Market | null>(null)

  // Form State
  const [marketName, setMarketName] = useState("")
  const [city, setCity] = useState("Ahmedabad")
  const [area, setArea] = useState("")
  const [pincode, setPincode] = useState("")
  const [landmark, setLandmark] = useState("")
  const [marketType, setMarketType] = useState("Readymade Garments & Wholesale")
  const [description, setDescription] = useState("")

  // Auto-seed Ahmedabad textile markets if RTDB markets node is currently empty
  useEffect(() => {
    if (markets.length === 0 && AHMEDABAD_TEXTILE_MARKETS.length > 0) {
      const seedMarkets = async () => {
        const topMarkets = AHMEDABAD_TEXTILE_MARKETS.slice(0, 10)
        for (let i = 0; i < topMarkets.length; i++) {
          const name = topMarkets[i]
          if (name.includes("Other")) continue
          await saveMarket({
            id: 100 + i + 1,
            marketName: name,
            city: name.includes("Surat") ? "Surat" : "Ahmedabad",
            area: name.includes("(") ? name.substring(name.indexOf("(") + 1, name.indexOf(")")) : "",
            pincode: "380002",
            marketType: "Wholesale Textile Cluster",
            createdAt: Date.now(),
          })
        }
      }
      seedMarkets()
    }
  }, [markets.length])

  const openAddModal = () => {
    setEditingMarket(null)
    setMarketName("")
    setCity("Ahmedabad")
    setArea("")
    setPincode("380002")
    setLandmark("")
    setMarketType("Readymade Garments & Wholesale")
    setDescription("")
    setIsModalOpen(true)
  }

  const openEditModal = (m: Market) => {
    setEditingMarket(m)
    setMarketName(m.marketName || "")
    setCity(m.city || "Ahmedabad")
    setArea(m.area || "")
    setPincode(m.pincode || "")
    setLandmark(m.landmark || "")
    setMarketType(m.marketType || "Readymade Garments & Wholesale")
    setDescription(m.description || "")
    setIsModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!marketName.trim()) return

    const payload: Market = {
      id: editingMarket?.id || Date.now(),
      marketName: marketName.trim(),
      city: city.trim(),
      area: area.trim(),
      pincode: pincode.trim(),
      landmark: landmark.trim(),
      marketType: marketType.trim(),
      description: description.trim(),
      createdAt: editingMarket?.createdAt || Date.now(),
    }

    await saveMarket(payload)
    setIsModalOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (confirm("Are you sure you want to delete this textile market?")) {
      await deleteMarket(id)
    }
  }

  const filteredMarkets = markets.filter((m) => {
    const q = search.toLowerCase()
    return (
      (m.marketName || "").toLowerCase().includes(q) ||
      (m.city || "").toLowerCase().includes(q) ||
      (m.area || "").toLowerCase().includes(q)
    )
  })

  // Count suppliers per market
  const supplierCountMap = React.useMemo(() => {
    const counts = new Map<string, number>()
    suppliers.forEach((s) => {
      const mName = s.marketName || s.marketArea || ""
      if (mName) {
        counts.set(mName, (counts.get(mName) || 0) + 1)
      }
    })
    return counts
  }, [suppliers])

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              Textile Markets Master
            </h2>
            <Badge variant="outline" className="text-xs bg-emerald-500/10 text-emerald-700 border-emerald-500/20 font-semibold">
              {markets.length} Markets
            </Badge>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Cloth markets, clusters, commercial hubs & procurement trade centers
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <Input
              type="text"
              placeholder="Search markets or areas..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 h-9 text-xs"
            />
          </div>
          <Button onClick={openAddModal} className="h-9 gap-1.5 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
            <Plus className="h-3.5 w-3.5" />
            Add Market
          </Button>
        </div>
      </div>

      {/* Grid of Markets */}
      {filteredMarkets.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12 text-center border-dashed">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-500 mb-3">
            <Compass className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No markets found</h3>
          <p className="text-xs text-zinc-500 max-w-sm mt-1">
            {search ? "No textile markets match your query." : "Register wholesale textile hubs and cloth markets."}
          </p>
          <Button onClick={openAddModal} variant="outline" className="mt-4 h-8 text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Register First Market
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredMarkets.map((market) => {
            const count = supplierCountMap.get(market.marketName) || 0
            return (
              <Card key={market.id} className="group relative overflow-hidden border border-zinc-200/80 p-4 transition-all hover:shadow-md dark:border-zinc-800">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-700 font-bold text-base dark:bg-emerald-500/20 dark:text-emerald-400">
                      <Compass className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-50 line-clamp-1">
                        {market.marketName}
                      </h4>
                      <span className="text-[11px] font-medium text-emerald-700 dark:text-emerald-400">
                        {market.city || "Ahmedabad"}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openEditModal(market)}
                      className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                    >
                      <Edit2 className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDelete(market.id)}
                      className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>

                <div className="mt-3 space-y-2 border-t border-zinc-100 pt-3 dark:border-zinc-800 text-xs">
                  {market.area && (
                    <div className="flex items-center gap-1.5 text-zinc-600 dark:text-zinc-400">
                      <MapPin className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                      <span>Area: {market.area} {market.pincode && `(${market.pincode})`}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-between pt-1">
                    <span className="text-[11px] text-zinc-500">
                      {market.marketType || "Textile Cluster"}
                    </span>
                    <Badge variant="outline" className="text-[10px] font-semibold bg-zinc-100 text-zinc-700 border-zinc-200">
                      {count} Suppliers Linked
                    </Badge>
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      )}

      {/* Add / Edit Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingMarket ? "Edit Market" : "Add Market Master"}
      >
        <form onSubmit={handleSave} className="space-y-3.5 text-xs">
          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Market Name *
            </label>
            <Input
              required
              placeholder="e.g. Maskati Cloth Market, New Cloth Market (Raipur)"
              value={marketName}
              onChange={(e) => setMarketName(e.target.value)}
              className="h-8 text-xs"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                City *
              </label>
              <Input
                required
                placeholder="e.g. Ahmedabad, Surat"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Pincode
              </label>
              <Input
                placeholder="e.g. 380002"
                value={pincode}
                onChange={(e) => setPincode(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Area / Sub-locality
              </label>
              <Input
                placeholder="e.g. Sakarkalupur, Raipur, Relief Rd"
                value={area}
                onChange={(e) => setArea(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Landmark
              </label>
              <Input
                placeholder="e.g. Near Kalupur Railway Station"
                value={landmark}
                onChange={(e) => setLandmark(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Market Cluster Type
            </label>
            <select
              value={marketType}
              onChange={(e) => setMarketType(e.target.value)}
              className="w-full h-8 rounded-md border border-zinc-300 bg-white px-3 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            >
              <option value="Readymade Garments & Wholesale">Readymade Garments & Wholesale</option>
              <option value="Fabric & Shirting/Suiting Wholesale">Fabric & Shirting/Suiting Wholesale</option>
              <option value="Sarees & Ethnic Wear Hub">Sarees & Ethnic Wear Hub</option>
              <option value="Denim & Bottom Wear Cluster">Denim & Bottom Wear Cluster</option>
              <option value="Mill Industrial GIDC Area">Mill Industrial GIDC Area</option>
              <option value="Mixed Garment Trade Center">Mixed Garment Trade Center</option>
            </select>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-zinc-200 dark:border-zinc-800">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)} className="h-8 text-xs">
              Cancel
            </Button>
            <Button type="submit" className="h-8 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
              {editingMarket ? "Save Changes" : "Create Market"}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
