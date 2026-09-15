import React, { useState } from "react"
import {
  Truck,
  Plus,
  Search,
  Phone,
  MapPin,
  FileText,
  Edit2,
  Trash2,
  ExternalLink,
  Building,
  Navigation
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Transporter } from "../types"

export function TransportersView() {
  const { transporters, saveTransporter, deleteTransporter } = useData()
  const [search, setSearch] = useState("")
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingTransporter, setEditingTransporter] = useState<Transporter | null>(null)

  // Form State
  const [transporterName, setTransporterName] = useState("")
  const [contactPerson, setContactPerson] = useState("")
  const [phone, setPhone] = useState("")
  const [phone2, setPhone2] = useState("")
  const [phone3, setPhone3] = useState("")
  const [officeAddress, setOfficeAddress] = useState("")
  const [godownAddress, setGodownAddress] = useState("")
  const [city, setCity] = useState("Ahmedabad")
  const [destinationsCovered, setDestinationsCovered] = useState("")
  const [gstin, setGstin] = useState("")
  const [trackingUrl, setTrackingUrl] = useState("")
  const [notes, setNotes] = useState("")

  const openAddModal = () => {
    setEditingTransporter(null)
    setTransporterName("")
    setContactPerson("")
    setPhone("")
    setPhone2("")
    setPhone3("")
    setOfficeAddress("")
    setGodownAddress("")
    setCity("Ahmedabad")
    setDestinationsCovered("")
    setGstin("")
    setTrackingUrl("")
    setNotes("")
    setIsModalOpen(true)
  }

  const openEditModal = (t: Transporter) => {
    setEditingTransporter(t)
    setTransporterName(t.transporterName || "")
    setContactPerson(t.contactPerson || "")
    setPhone(t.phone || "")
    setPhone2(t.phone2 || "")
    setPhone3(t.phone3 || "")
    setOfficeAddress(t.officeAddress || "")
    setGodownAddress(t.godownAddress || "")
    setCity(t.city || "Ahmedabad")
    setDestinationsCovered(t.destinationsCovered || "")
    setGstin(t.gstin || "")
    setTrackingUrl(t.trackingUrl || "")
    setNotes(t.notes || "")
    setIsModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!transporterName.trim() || !phone.trim()) return

    const payload: Transporter = {
      id: editingTransporter?.id || Date.now(),
      transporterName: transporterName.trim(),
      contactPerson: contactPerson.trim(),
      phone: phone.trim(),
      phone2: phone2.trim(),
      phone3: phone3.trim(),
      officeAddress: officeAddress.trim(),
      godownAddress: godownAddress.trim(),
      city: city.trim(),
      destinationsCovered: destinationsCovered.trim(),
      gstin: gstin.trim().toUpperCase(),
      trackingUrl: trackingUrl.trim(),
      notes: notes.trim(),
      createdAt: editingTransporter?.createdAt || Date.now(),
    }

    await saveTransporter(payload)
    setIsModalOpen(false)
  }

  const handleDelete = async (id: number) => {
    if (confirm("Are you sure you want to delete this transporter?")) {
      await deleteTransporter(id)
    }
  }

  const filteredTransporters = transporters.filter((t) => {
    const q = search.toLowerCase()
    return (
      (t.transporterName || "").toLowerCase().includes(q) ||
      (t.contactPerson || "").toLowerCase().includes(q) ||
      (t.phone || "").toLowerCase().includes(q) ||
      (t.city || "").toLowerCase().includes(q) ||
      (t.destinationsCovered || "").toLowerCase().includes(q)
    )
  })

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              Transporter & Courier Master
            </h2>
            <Badge variant="outline" className="text-xs bg-blue-500/10 text-blue-700 border-blue-500/20 font-semibold">
              {transporters.length} Transporters
            </Badge>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Logistics partners, transport godowns, booking booking contacts & delivery routes
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative w-64">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <Input
              type="text"
              placeholder="Search transporters, phone, routes..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 h-9 text-xs"
            />
          </div>
          <Button onClick={openAddModal} className="h-9 gap-1.5 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
            <Plus className="h-3.5 w-3.5" />
            Add Transporter
          </Button>
        </div>
      </div>

      {/* Grid of Transporters */}
      {filteredTransporters.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12 text-center border-dashed">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-500 mb-3">
            <Truck className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No transporters found</h3>
          <p className="text-xs text-zinc-500 max-w-sm mt-1">
            {search ? "No records match your search query." : "Register transport services and logistics agencies for parcel dispatches across India."}
          </p>
          <Button onClick={openAddModal} variant="outline" className="mt-4 h-8 text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Register First Transporter
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredTransporters.map((trans) => (
            <Card key={trans.id} className="group relative overflow-hidden border border-zinc-200/80 p-4 transition-all hover:shadow-md dark:border-zinc-800">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10 text-blue-700 font-bold text-base dark:bg-blue-500/20 dark:text-blue-400">
                    <Truck className="h-5 w-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-50 line-clamp-1">
                      {trans.transporterName}
                    </h4>
                    <span className="text-[11px] font-medium text-zinc-500 dark:text-zinc-400">
                      {trans.city || "Ahmedabad Hub"}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => openEditModal(trans)}
                    className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                  >
                    <Edit2 className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDelete(trans.id)}
                    className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>

              <div className="mt-3 space-y-2 border-t border-zinc-100 pt-3 dark:border-zinc-800 text-xs">
                {trans.contactPerson && (
                  <div className="text-zinc-700 dark:text-zinc-300 font-medium">
                    Contact: {trans.contactPerson}
                  </div>
                )}

                <div className="flex items-center gap-2 text-zinc-600 dark:text-zinc-400">
                  <Phone className="h-3.5 w-3.5 text-zinc-400 shrink-0" />
                  <span>{trans.phone}</span>
                  {trans.phone2 && <span className="text-zinc-400">• {trans.phone2}</span>}
                </div>

                {trans.officeAddress && (
                  <div className="flex items-start gap-1.5 text-zinc-600 dark:text-zinc-400">
                    <Building className="h-3.5 w-3.5 text-zinc-400 shrink-0 mt-0.5" />
                    <span className="line-clamp-1">Office: {trans.officeAddress}</span>
                  </div>
                )}

                {trans.godownAddress && (
                  <div className="flex items-start gap-1.5 text-zinc-600 dark:text-zinc-400">
                    <MapPin className="h-3.5 w-3.5 text-zinc-400 shrink-0 mt-0.5" />
                    <span className="line-clamp-1">Godown: {trans.godownAddress}</span>
                  </div>
                )}

                {trans.destinationsCovered && (
                  <div className="flex items-start gap-1.5 text-zinc-600 dark:text-zinc-400">
                    <Navigation className="h-3.5 w-3.5 text-blue-500 shrink-0 mt-0.5" />
                    <span className="line-clamp-1 font-medium text-blue-700 dark:text-blue-400">
                      Routes: {trans.destinationsCovered}
                    </span>
                  </div>
                )}

                {trans.gstin && (
                  <div className="text-[11px] font-mono text-zinc-500">
                    GSTIN: {trans.gstin}
                  </div>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Add / Edit Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingTransporter ? "Edit Transporter" : "Add Transporter Master"}
      >
        <form onSubmit={handleSave} className="space-y-3.5 text-xs max-h-[78vh] overflow-y-auto pr-1">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Transporter Name *
              </label>
              <Input
                required
                placeholder="e.g. VRL Logistics, TCI Express"
                value={transporterName}
                onChange={(e) => setTransporterName(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Contact Person
              </label>
              <Input
                placeholder="Branch Manager / In-charge"
                value={contactPerson}
                onChange={(e) => setContactPerson(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Primary Phone *
              </label>
              <Input
                required
                placeholder="+91 98..."
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Phone 2 (Optional)
              </label>
              <Input
                placeholder="Alternate phone"
                value={phone2}
                onChange={(e) => setPhone2(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Phone 3 (Optional)
              </label>
              <Input
                placeholder="Booking line"
                value={phone3}
                onChange={(e) => setPhone3(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                City / Hub
              </label>
              <Input
                placeholder="e.g. Ahmedabad, Surat"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="h-8 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                GSTIN / Transporter ID
              </label>
              <Input
                placeholder="15-character GSTIN"
                value={gstin}
                onChange={(e) => setGstin(e.target.value)}
                className="h-8 text-xs font-mono uppercase"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Booking Office Address
            </label>
            <Input
              placeholder="e.g. Shop 12, Transport Nagar, Narol, Ahmedabad"
              value={officeAddress}
              onChange={(e) => setOfficeAddress(e.target.value)}
              className="h-8 text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Godown / Delivery Hub Address
            </label>
            <Input
              placeholder="e.g. Godown #4, Aslali Ring Road, Ahmedabad"
              value={godownAddress}
              onChange={(e) => setGodownAddress(e.target.value)}
              className="h-8 text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Destinations & States Covered
            </label>
            <Input
              placeholder="e.g. UP, Bihar, Delhi NCR, Rajasthan, MP"
              value={destinationsCovered}
              onChange={(e) => setDestinationsCovered(e.target.value)}
              className="h-8 text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Online Tracking Website URL (Optional)
            </label>
            <Input
              placeholder="https://..."
              value={trackingUrl}
              onChange={(e) => setTrackingUrl(e.target.value)}
              className="h-8 text-xs"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-zinc-200 dark:border-zinc-800">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)} className="h-8 text-xs">
              Cancel
            </Button>
            <Button type="submit" className="h-8 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
              {editingTransporter ? "Save Changes" : "Create Transporter"}
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
