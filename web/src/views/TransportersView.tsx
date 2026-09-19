import React, { useState, useEffect } from "react"
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
  Navigation,
  Info,
  Eye
} from "lucide-react"
import { useData } from "../context/DataContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { Transporter } from "../types"
import { getMasterDraft, saveMasterDraft, clearMasterDraft } from "../lib/masterDrafts"
import { TransporterDetailView } from "./TransporterDetailView"

export function TransportersView() {
  const { transporters, saveTransporter, deleteTransporter } = useData()
  const [search, setSearch] = useState("")
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedTransporterId, setSelectedTransporterId] = useState<number | null>(null)
  const [editingTransporter, setEditingTransporter] = useState<Transporter | null>(null)
  const [hasDraft, setHasDraft] = useState(false)

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
    const draft = getMasterDraft<any>("transporter")
    if (draft) {
      setTransporterName(draft.transporterName || "")
      setContactPerson(draft.contactPerson || "")
      setPhone(draft.phone || "")
      setPhone2(draft.phone2 || "")
      setPhone3(draft.phone3 || "")
      setOfficeAddress(draft.officeAddress || "")
      setGodownAddress(draft.godownAddress || "")
      setCity(draft.city || "Ahmedabad")
      setDestinationsCovered(draft.destinationsCovered || "")
      setGstin(draft.gstin || "")
      setTrackingUrl(draft.trackingUrl || "")
      setNotes(draft.notes || "")
      setHasDraft(true)
    } else {
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
      setHasDraft(false)
    }
    setIsModalOpen(true)
  }

  const handleDiscardDraft = () => {
    clearMasterDraft("transporter")
    setHasDraft(false)
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
  }

  // Auto-save local draft
  useEffect(() => {
    if (!isModalOpen || editingTransporter !== null) return
    if (transporterName.trim() || phone.trim() || contactPerson.trim() || destinationsCovered.trim()) {
      saveMasterDraft("transporter", {
        transporterName,
        contactPerson,
        phone,
        phone2,
        phone3,
        officeAddress,
        godownAddress,
        city,
        destinationsCovered,
        gstin,
        trackingUrl,
        notes,
      })
    }
  }, [
    isModalOpen,
    editingTransporter,
    transporterName,
    contactPerson,
    phone,
    phone2,
    phone3,
    officeAddress,
    godownAddress,
    city,
    destinationsCovered,
    gstin,
    trackingUrl,
    notes,
  ])

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
    setHasDraft(false)
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
    clearMasterDraft("transporter")
    setHasDraft(false)
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

  if (selectedTransporterId !== null) {
    return (
      <TransporterDetailView
        transporterId={selectedTransporterId}
        onBack={() => setSelectedTransporterId(null)}
        onEdit={(t) => openEditModal(t)}
      />
    )
  }

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

      {/* Transporters Master List Table */}
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
        <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                <tr>
                  <th className="py-3 px-4">Transporter & Hub</th>
                  <th className="py-3 px-4">Contact Person</th>
                  <th className="py-3 px-4">Phone / Mobile</th>
                  <th className="py-3 px-4">Routes / Destinations</th>
                  <th className="py-3 px-4">Office & Godown</th>
                  <th className="py-3 px-4">GSTIN</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {filteredTransporters.map((trans) => (
                  <tr
                    key={trans.id}
                    onClick={() => setSelectedTransporterId(trans.id)}
                    className="hover:bg-blue-50/40 dark:hover:bg-blue-950/20 cursor-pointer transition-colors group"
                  >
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-blue-700 font-bold text-sm dark:bg-blue-500/20 dark:text-blue-400 group-hover:bg-blue-600 group-hover:text-white transition-colors">
                          <Truck className="h-4 w-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold text-zinc-900 dark:text-zinc-100 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                            {trans.transporterName}
                          </div>
                          <div className="text-[11px] text-zinc-400">
                            {trans.city || "Ahmedabad Hub"}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-medium text-zinc-700 dark:text-zinc-300">
                        {trans.contactPerson || "—"}
                      </span>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-1.5 font-medium text-zinc-800 dark:text-zinc-200">
                          <Phone className="h-3 w-3 text-zinc-400 shrink-0" />
                          <span>{trans.phone}</span>
                        </div>
                        {trans.phone2 && (
                          <div className="text-[11px] text-zinc-400 pl-4.5">
                            {trans.phone2}
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      {trans.destinationsCovered ? (
                        <div className="flex items-start gap-1 text-blue-700 dark:text-blue-400 font-medium">
                          <Navigation className="h-3 w-3 shrink-0 mt-0.5" />
                          <span className="line-clamp-2 max-w-xs">{trans.destinationsCovered}</span>
                        </div>
                      ) : (
                        <span className="text-zinc-400">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <div className="space-y-0.5 max-w-xs text-[11px] text-zinc-500">
                        {trans.officeAddress && (
                          <div className="truncate" title={trans.officeAddress}>
                            <span className="font-semibold text-zinc-600 dark:text-zinc-400">Off:</span> {trans.officeAddress}
                          </div>
                        )}
                        {trans.godownAddress && (
                          <div className="truncate text-zinc-400" title={trans.godownAddress}>
                            <span className="font-semibold text-zinc-500">Godown:</span> {trans.godownAddress}
                          </div>
                        )}
                        {!trans.officeAddress && !trans.godownAddress && <span>—</span>}
                      </div>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      {trans.gstin ? (
                        <span className="font-mono text-[11px] text-zinc-500">{trans.gstin}</span>
                      ) : (
                        <span className="text-zinc-400">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedTransporterId(trans.id)}
                          className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                          title="View Dedicated Details & Orders"
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openEditModal(trans)}
                          className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                          title="Edit Transporter"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(trans.id)}
                          className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                          title="Delete Transporter"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add / Edit Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingTransporter ? "Edit Transporter" : "Add Transporter Master"}
      >
        <div className="space-y-3.5 max-h-[78vh] overflow-y-auto pr-1">
          {/* Draft Notification Banner */}
          {hasDraft && !editingTransporter && (
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

          <form onSubmit={handleSave} className="space-y-3.5 text-xs">
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
        </div>
      </Dialog>
    </div>
  )
}
