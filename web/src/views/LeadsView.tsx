import React, { useState, useMemo } from "react"
import {
  Users,
  Plus,
  Search,
  Phone,
  MessageCircle,
  MapPin,
  Building2,
  Tag,
  Edit2,
  Trash2,
  Sparkles,
  CheckCircle2,
  Clock,
  ExternalLink,
  Info,
  Calendar,
  Image as ImageIcon,
  Check,
  UserCheck
} from "lucide-react"
import { useData } from "../context/DataContext"
import { useAuth } from "../context/AuthContext"
import { Card } from "../components/ui/Card"
import { Button } from "../components/ui/Button"
import { Badge } from "../components/ui/Badge"
import { Dialog } from "../components/ui/Dialog"
import { Input } from "../components/ui/Input"
import { FileUpload } from "../components/ui/FileUpload"
import { ImageLightboxModal } from "../components/ui/ImageLightboxModal"
import { Lead, LeadType, LeadStatus } from "../types"

export function LeadsView() {
  const { leads, customerLeads, supplierLeads, saveLead, deleteLead, convertLeadToMaster } = useData()
  const { user } = useAuth()

  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState<"all" | "customer" | "supplier">("all")
  const [statusFilter, setStatusFilter] = useState<"all" | LeadStatus>("all")

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingLead, setEditingLead] = useState<Lead | null>(null)
  const [convertingLead, setConvertingLead] = useState<Lead | null>(null)

  // Lightbox State
  const [lightbox, setLightbox] = useState<{ open: boolean; url: string; title: string }>({
    open: false,
    url: "",
    title: "",
  })

  // Form State
  const [leadType, setLeadType] = useState<LeadType>("customer")
  const [name, setName] = useState("")
  const [firmName, setFirmName] = useState("")
  const [supplierType, setSupplierType] = useState<"Manufacturer" | "Wholesaler">("Manufacturer")
  const [phone, setPhone] = useState("")
  const [phone2, setPhone2] = useState("")
  const [meetingPlace, setMeetingPlace] = useState("")
  const [city, setCity] = useState("Ahmedabad")
  const [notes, setNotes] = useState("")
  const [status, setStatus] = useState<LeadStatus>("Thinking")
  const [nextFollowUpDate, setNextFollowUpDate] = useState("")
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([])
  const [currentPhotoInput, setCurrentPhotoInput] = useState("")

  const openAddModal = () => {
    setEditingLead(null)
    setLeadType("customer")
    setName("")
    setFirmName("")
    setSupplierType("Manufacturer")
    setPhone("")
    setPhone2("")
    setMeetingPlace("")
    setCity("Ahmedabad")
    setNotes("")
    setStatus("Thinking")
    setNextFollowUpDate("")
    setUploadedPhotos([])
    setCurrentPhotoInput("")
    setIsModalOpen(true)
  }

  const openEditModal = (lead: Lead) => {
    setEditingLead(lead)
    setLeadType(lead.type || "customer")
    setName(lead.name || "")
    setFirmName(lead.firmName || "")
    setSupplierType(lead.supplierType || "Manufacturer")
    setPhone(lead.phone || "")
    setPhone2(lead.phone2 || "")
    setMeetingPlace(lead.meetingPlace || "")
    setCity(lead.city || "Ahmedabad")
    setNotes(lead.notes || "")
    setStatus(lead.status || "Thinking")
    setNextFollowUpDate(lead.nextFollowUpDate || "")
    setUploadedPhotos(lead.photos || [])
    setCurrentPhotoInput("")
    setIsModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!firmName.trim() || !phone.trim()) {
      alert("Please provide both Firm/Shop Name and Phone number.")
      return
    }

    const photosList = [...uploadedPhotos]
    if (currentPhotoInput.trim() && !photosList.includes(currentPhotoInput.trim())) {
      photosList.push(currentPhotoInput.trim())
    }

    try {
      await saveLead({
        id: editingLead?.id,
        leadId: editingLead?.leadId,
        type: leadType,
        name: name.trim(),
        firmName: firmName.trim(),
        supplierType: leadType === "supplier" ? supplierType : undefined,
        phone: phone.trim(),
        phone2: phone2.trim(),
        meetingPlace: meetingPlace.trim(),
        city: city.trim(),
        notes: notes.trim(),
        photos: photosList,
        status: status,
        nextFollowUpDate: nextFollowUpDate,
        createdAt: editingLead?.createdAt,
        createdByUid: editingLead?.createdByUid || user?.uid || "",
        createdByName: editingLead?.createdByName || user?.displayName || user?.email || "Staff",
      })
      setIsModalOpen(false)
    } catch (err: any) {
      alert(`Failed to save lead: ${err.message}`)
    }
  }

  const handleDelete = async (lead: Lead) => {
    if (window.confirm(`Are you sure you want to delete lead "${lead.firmName}"?`)) {
      try {
        await deleteLead(lead.id)
      } catch (err: any) {
        alert(`Failed to delete lead: ${err.message}`)
      }
    }
  }

  const handleConvert = async (lead: Lead) => {
    try {
      await convertLeadToMaster(lead, lead.type)
      setConvertingLead(null)
      alert(`Lead "${lead.firmName}" successfully converted to active ${lead.type === "customer" ? "Customer" : "Supplier"}!`)
    } catch (err: any) {
      alert(`Failed to convert lead: ${err.message}`)
    }
  }

  // Filtered Leads
  const filteredLeads = useMemo(() => {
    return leads.filter((lead) => {
      // Type Filter
      if (typeFilter !== "all" && lead.type !== typeFilter) return false

      // Status Filter
      if (statusFilter !== "all" && lead.status !== statusFilter) return false

      // Search Query
      if (search.trim()) {
        const q = search.toLowerCase()
        const nameMatch = lead.name?.toLowerCase().includes(q)
        const firmMatch = lead.firmName?.toLowerCase().includes(q)
        const phoneMatch = lead.phone?.includes(q) || lead.phone2?.includes(q)
        const placeMatch = lead.meetingPlace?.toLowerCase().includes(q) || lead.city?.toLowerCase().includes(q)
        const notesMatch = lead.notes?.toLowerCase().includes(q)
        return nameMatch || firmMatch || phoneMatch || placeMatch || notesMatch
      }

      return true
    })
  }, [leads, typeFilter, statusFilter, search])

  const thinkingCount = useMemo(() => leads.filter((l) => l.status === "Thinking").length, [leads])
  const followUpCount = useMemo(() => leads.filter((l) => l.status === "Follow-up").length, [leads])
  const convertedCount = useMemo(() => leads.filter((l) => l.status === "Converted").length, [leads])

  return (
    <div className="flex-1 space-y-5 p-4 md:p-6 overflow-y-auto">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50">
              Leads & Market Prospects (CRM)
            </h1>
            <Badge variant="outline" className="text-[11px] font-semibold bg-amber-50 text-amber-700 border-amber-200">
              {leads.length} Total Prospects
            </Badge>
          </div>
          <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            Contacts met in textile markets or trade visits who are not yet customers or suppliers (still thinking / follow-up).
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button onClick={openAddModal} size="sm" className="h-9 gap-1.5 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
            <Plus className="h-4 w-4" />
            Add New Lead
          </Button>
        </div>
      </div>

      {/* KPI Metrics */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <Card className="p-3.5 border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <span className="text-[11px] font-medium text-zinc-500">Retailer Leads</span>
            <div className="text-xl font-bold text-blue-600 mt-0.5">{customerLeads.length}</div>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 dark:bg-blue-950/30 text-blue-600">
            <Users className="h-4 w-4" />
          </div>
        </Card>

        <Card className="p-3.5 border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <span className="text-[11px] font-medium text-zinc-500">Supplier Leads</span>
            <div className="text-xl font-bold text-amber-600 mt-0.5">{supplierLeads.length}</div>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 dark:bg-amber-950/30 text-amber-600">
            <Building2 className="h-4 w-4" />
          </div>
        </Card>

        <Card className="p-3.5 border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <span className="text-[11px] font-medium text-zinc-500">Thinking / Deciding</span>
            <div className="text-xl font-bold text-orange-600 mt-0.5">{thinkingCount}</div>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-orange-50 dark:bg-orange-950/30 text-orange-600">
            <Clock className="h-4 w-4" />
          </div>
        </Card>

        <Card className="p-3.5 border-zinc-200 dark:border-zinc-800 flex items-center justify-between">
          <div>
            <span className="text-[11px] font-medium text-zinc-500">Converted to Master</span>
            <div className="text-xl font-bold text-emerald-600 mt-0.5">{convertedCount}</div>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600">
            <CheckCircle2 className="h-4 w-4" />
          </div>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between bg-white dark:bg-zinc-900 p-3 rounded-xl border border-zinc-200 dark:border-zinc-800">
        <div className="flex items-center gap-1.5 flex-wrap">
          {/* Type Filter Buttons */}
          <div className="flex items-center gap-1 bg-zinc-100 dark:bg-zinc-800 p-1 rounded-lg">
            <button
              onClick={() => setTypeFilter("all")}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                typeFilter === "all"
                  ? "bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 shadow-xs"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              All ({leads.length})
            </button>
            <button
              onClick={() => setTypeFilter("customer")}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                typeFilter === "customer"
                  ? "bg-white dark:bg-zinc-900 text-blue-600 shadow-xs"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              Retailers ({customerLeads.length})
            </button>
            <button
              onClick={() => setTypeFilter("supplier")}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-colors ${
                typeFilter === "supplier"
                  ? "bg-white dark:bg-zinc-900 text-amber-600 shadow-xs"
                  : "text-zinc-600 dark:text-zinc-400 hover:text-zinc-900"
              }`}
            >
              Suppliers ({supplierLeads.length})
            </button>
          </div>

          {/* Status Filter Chips */}
          <div className="hidden sm:flex items-center gap-1 pl-2 border-l border-zinc-200 dark:border-zinc-800">
            {(["all", "Thinking", "Follow-up", "New", "Converted"] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 text-[11px] font-medium rounded-full transition-colors ${
                  statusFilter === st
                    ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                    : "text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                }`}
              >
                {st === "all" ? "Any Status" : st}
              </button>
            ))}
          </div>
        </div>

        {/* Search */}
        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-zinc-400" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by shop, name, phone, meeting place..."
            className="pl-9 h-9 text-xs"
          />
        </div>
      </div>

      {/* Master List Table */}
      {filteredLeads.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12 text-center border-dashed">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800 text-zinc-500 mb-3">
            <Users className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">No leads found</h3>
          <p className="text-xs text-zinc-500 max-w-sm mt-1">
            {search ? "No leads match your filter criteria." : "Start saving prospective customers and suppliers you meet on market visits."}
          </p>
          <Button onClick={openAddModal} variant="outline" className="mt-4 h-8 text-xs gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Add First Lead
          </Button>
        </Card>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50/80 dark:bg-zinc-900/80 text-[11px] font-bold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
                <tr>
                  <th className="py-3 px-4">Lead / Shop & Contact</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Phone & WhatsApp</th>
                  <th className="py-3 px-4">Met At & City</th>
                  <th className="py-3 px-4">Discussion & Notes</th>
                  <th className="py-3 px-4">Photos</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800/60">
                {filteredLeads.map((lead) => (
                  <tr
                    key={lead.id}
                    className="hover:bg-zinc-50/60 dark:hover:bg-zinc-800/30 transition-colors group"
                  >
                    {/* Name & Firm */}
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2.5">
                        <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl font-bold text-sm ${
                          lead.type === "supplier"
                            ? "bg-amber-500/10 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400"
                            : "bg-blue-500/10 text-blue-700 dark:bg-blue-500/20 dark:text-blue-400"
                        }`}>
                          {(lead.firmName || lead.name || "L")[0].toUpperCase()}
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold text-zinc-900 dark:text-zinc-100">
                            {lead.firmName}
                          </div>
                          <div className="text-[11px] text-zinc-500 flex items-center gap-1">
                            <span>{lead.name || "Owner"}</span>
                            {lead.leadId && <span className="text-zinc-400 font-mono">• {lead.leadId}</span>}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Type Badge */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      {lead.type === "supplier" ? (
                        <div className="space-y-0.5">
                          <Badge variant="outline" className="text-[10px] font-bold text-amber-700 border-amber-200 bg-amber-50 dark:bg-amber-950/30">
                            Supplier
                          </Badge>
                          {lead.supplierType && (
                            <div className="text-[10px] text-zinc-500 font-medium">
                              {lead.supplierType}
                            </div>
                          )}
                        </div>
                      ) : (
                        <Badge variant="outline" className="text-[10px] font-bold text-blue-700 border-blue-200 bg-blue-50 dark:bg-blue-950/30">
                          Retailer
                        </Badge>
                      )}
                    </td>

                    {/* Phone & WhatsApp */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="space-y-1">
                        <div className="flex items-center gap-1.5 font-medium text-zinc-800 dark:text-zinc-200">
                          <Phone className="h-3 w-3 text-zinc-400 shrink-0" />
                          <a href={`tel:${lead.phone}`} className="hover:underline text-zinc-900 dark:text-zinc-100">
                            {lead.phone}
                          </a>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <a
                            href={`https://wa.me/91${lead.phone.replace(/\D/g, "").slice(-10)}?text=${encodeURIComponent(
                              `Hello ${lead.name || lead.firmName}, regards from Himat Textile.`
                            )}`}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-[11px] text-emerald-600 hover:text-emerald-700 font-medium"
                          >
                            <MessageCircle className="h-3 w-3" />
                            <span>WhatsApp</span>
                          </a>
                          {lead.phone2 && (
                            <span className="text-[10px] text-zinc-400">• {lead.phone2}</span>
                          )}
                        </div>
                      </div>
                    </td>

                    {/* Met At & City */}
                    <td className="py-3 px-4">
                      <div className="space-y-0.5">
                        {lead.meetingPlace ? (
                          <div className="flex items-center gap-1 text-zinc-800 dark:text-zinc-200 font-medium">
                            <MapPin className="h-3 w-3 text-red-500 shrink-0" />
                            <span className="truncate max-w-[180px]" title={lead.meetingPlace}>
                              {lead.meetingPlace}
                            </span>
                          </div>
                        ) : (
                          <span className="text-zinc-400 italic">No place entered</span>
                        )}
                        <div className="text-[11px] text-zinc-500 pl-4">
                          {lead.city || "Ahmedabad"}
                        </div>
                      </div>
                    </td>

                    {/* Discussion & Notes */}
                    <td className="py-3 px-4 max-w-xs">
                      {lead.notes ? (
                        <p className="text-[11px] text-zinc-600 dark:text-zinc-300 line-clamp-2 italic" title={lead.notes}>
                          "{lead.notes}"
                        </p>
                      ) : (
                        <span className="text-zinc-400">—</span>
                      )}
                      {lead.createdByName && (
                        <div className="text-[10px] text-zinc-400 mt-1">
                          By {lead.createdByName} • {lead.createdAt ? new Date(lead.createdAt).toLocaleDateString() : ""}
                        </div>
                      )}
                    </td>

                    {/* Photos */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      {lead.photos && lead.photos.length > 0 ? (
                        <div className="flex items-center gap-1.5">
                          {lead.photos.slice(0, 3).map((photoUri, idx) => (
                            <div
                              key={idx}
                              onClick={() =>
                                setLightbox({
                                  open: true,
                                  url: photoUri,
                                  title: `${lead.firmName} - Photo ${idx + 1}`,
                                })
                              }
                              className="h-8 w-8 rounded-lg overflow-hidden border border-zinc-200 dark:border-zinc-700 cursor-pointer hover:ring-2 hover:ring-indigo-500 transition-all shrink-0"
                              title="Click to zoom photo"
                            >
                              <img src={photoUri} alt="Lead photo" className="h-full w-full object-cover" />
                            </div>
                          ))}
                          {lead.photos.length > 3 && (
                            <span className="text-[10px] font-bold text-zinc-400">+{lead.photos.length - 3}</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-zinc-400 text-[11px]">No photos</span>
                      )}
                    </td>

                    {/* Status */}
                    <td className="py-3 px-4 whitespace-nowrap">
                      <Badge
                        variant="outline"
                        className={`text-[10px] font-bold ${
                          lead.status === "Converted"
                            ? "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/30 dark:text-emerald-400"
                            : lead.status === "Thinking"
                            ? "bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-950/30 dark:text-orange-400"
                            : lead.status === "Follow-up"
                            ? "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-950/30 dark:text-purple-400"
                            : "bg-zinc-50 text-zinc-700 border-zinc-200"
                        }`}
                      >
                        {lead.status || "Thinking"}
                      </Badge>
                      {lead.nextFollowUpDate && (
                        <div className="text-[10px] text-zinc-400 mt-0.5 flex items-center gap-1">
                          <Calendar className="h-2.5 w-2.5" />
                          <span>{lead.nextFollowUpDate}</span>
                        </div>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                        {lead.status !== "Converted" && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setConvertingLead(lead)}
                            className="h-7 text-xs px-2 text-emerald-700 dark:text-emerald-400 border-emerald-200 hover:bg-emerald-50 dark:hover:bg-emerald-950/20 font-medium"
                            title={`Convert into active ${lead.type === "customer" ? "Customer" : "Supplier"}`}
                          >
                            <Sparkles className="h-3 w-3 mr-1 text-emerald-600" />
                            Convert
                          </Button>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => openEditModal(lead)}
                          className="h-7 w-7 p-0 text-zinc-500 hover:text-zinc-900"
                          title="Edit Lead"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleDelete(lead)}
                          className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                          title="Delete Lead"
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

      {/* Add / Edit Lead Dialog */}
      <Dialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        title={editingLead ? "Edit Lead / Prospect" : "Record New Market Lead"}
        description="Save details of a prospective retailer or fabric supplier met during market visits."
      >
        <form onSubmit={handleSave} className="space-y-4 pt-1 max-h-[80vh] overflow-y-auto pr-1">
          {/* Lead Type Toggle */}
          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1.5">
              Prospect Type *
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setLeadType("customer")}
                className={`flex items-center justify-center gap-2 p-2.5 rounded-xl border text-xs font-semibold transition-all ${
                  leadType === "customer"
                    ? "border-blue-600 bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300 ring-1 ring-blue-600"
                    : "border-zinc-200 dark:border-zinc-800 text-zinc-600 hover:bg-zinc-50"
                }`}
              >
                <Users className="h-4 w-4" />
                <span>Retailer (Customer Lead)</span>
              </button>
              <button
                type="button"
                onClick={() => setLeadType("supplier")}
                className={`flex items-center justify-center gap-2 p-2.5 rounded-xl border text-xs font-semibold transition-all ${
                  leadType === "supplier"
                    ? "border-amber-600 bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300 ring-1 ring-amber-600"
                    : "border-zinc-200 dark:border-zinc-800 text-zinc-600 hover:bg-zinc-50"
                }`}
              >
                <Building2 className="h-4 w-4" />
                <span>Supplier / Fabric Mill</span>
              </button>
            </div>
          </div>

          {/* If Supplier: Manufacturer vs Wholesaler */}
          {leadType === "supplier" && (
            <div className="p-3 bg-amber-50/60 dark:bg-amber-950/30 rounded-xl border border-amber-200 dark:border-amber-800/60">
              <label className="block text-xs font-semibold text-amber-900 dark:text-amber-200 mb-1.5">
                Supplier Category *
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setSupplierType("Manufacturer")}
                  className={`p-2 rounded-lg text-xs font-semibold border transition-all ${
                    supplierType === "Manufacturer"
                      ? "bg-amber-600 text-white border-amber-600"
                      : "bg-white text-zinc-700 border-zinc-200 dark:bg-zinc-900 dark:text-zinc-300"
                  }`}
                >
                  Fabric Mill / Manufacturer
                </button>
                <button
                  type="button"
                  onClick={() => setSupplierType("Wholesaler")}
                  className={`p-2 rounded-lg text-xs font-semibold border transition-all ${
                    supplierType === "Wholesaler"
                      ? "bg-amber-600 text-white border-amber-600"
                      : "bg-white text-zinc-700 border-zinc-200 dark:bg-zinc-900 dark:text-zinc-300"
                  }`}
                >
                  Wholesaler / Trader
                </button>
              </div>
            </div>
          )}

          {/* Firm Name & Contact Person */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Shop / Firm Name *
              </label>
              <Input
                required
                value={firmName}
                onChange={(e) => setFirmName(e.target.value)}
                placeholder="e.g. Mahavir Textiles, Gujarat Garments"
                className="h-9 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Contact Person / Owner Name
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Ramesh Bhai, Harish Patel"
                className="h-9 text-xs"
              />
            </div>
          </div>

          {/* Phone Numbers */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Primary Mobile Phone *
              </label>
              <Input
                required
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="10-digit mobile number"
                className="h-9 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                WhatsApp / Secondary Phone
              </label>
              <Input
                type="tel"
                value={phone2}
                onChange={(e) => setPhone2(e.target.value)}
                placeholder="Optional secondary phone"
                className="h-9 text-xs"
              />
            </div>
          </div>

          {/* Meeting Place & City */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Where Did You Meet Him? *
              </label>
              <Input
                required
                value={meetingPlace}
                onChange={(e) => setMeetingPlace(e.target.value)}
                placeholder="e.g. Kalupur Cloth Market, His shop, Surat Mill"
                className="h-9 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                City Hub
              </label>
              <Input
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="e.g. Ahmedabad, Surat, Mumbai"
                className="h-9 text-xs"
              />
            </div>
          </div>

          {/* Status & Next Follow-up */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Current Status
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as LeadStatus)}
                className="w-full h-9 rounded-md border border-zinc-300 bg-white px-3 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
              >
                <option value="Thinking">Thinking / Deciding (Not Ready Yet)</option>
                <option value="Follow-up">Follow-up Required (Call Later)</option>
                <option value="New">New Contact</option>
                <option value="Converted">Ready / Converted</option>
                <option value="Dropped">Dropped / Not Interested</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Next Follow-up Date (Optional)
              </label>
              <Input
                type="date"
                value={nextFollowUpDate}
                onChange={(e) => setNextFollowUpDate(e.target.value)}
                className="h-9 text-xs"
              />
            </div>
          </div>

          {/* Discussion Notes */}
          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Discussion Notes & Requirements
            </label>
            <textarea
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="What did they say? Looking for cotton fabrics, payment terms, minimum case size, expected order date..."
              className="w-full rounded-md border border-zinc-300 bg-white p-2.5 text-xs text-zinc-900 focus:border-zinc-900 focus:outline-none dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-100"
            />
          </div>

          {/* Photo Uploads */}
          <div>
            <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
              Attach Photos (Visiting Card, Shop Front, Samples)
            </label>
            <FileUpload
              label="Upload Visiting Card or Shop Front Photo"
              folder={`leads/${(firmName || "general").trim().replace(/\s+/g, "_")}`}
              prefix="lead_doc"
              value={currentPhotoInput}
              onChange={(url) => {
                if (url && !uploadedPhotos.includes(url)) {
                  setUploadedPhotos((prev) => [...prev, url])
                }
              }}
            />

            {uploadedPhotos.length > 0 && (
              <div className="flex items-center gap-2 mt-2 flex-wrap">
                {uploadedPhotos.map((url, idx) => (
                  <div key={idx} className="relative group h-12 w-12 rounded-lg border overflow-hidden">
                    <img src={url} alt="thumbnail" className="h-full w-full object-cover" />
                    <button
                      type="button"
                      onClick={() => setUploadedPhotos((prev) => prev.filter((p) => p !== url))}
                      className="absolute inset-0 bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity text-[10px]"
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-zinc-200 dark:border-zinc-800">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)} className="h-9 text-xs">
              Cancel
            </Button>
            <Button type="submit" className="h-9 text-xs bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-zinc-100 dark:text-zinc-900">
              {editingLead ? "Save Changes" : "Create Lead"}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Convert Lead Confirmation Dialog */}
      {convertingLead && (
        <Dialog
          open={Boolean(convertingLead)}
          onOpenChange={(open) => !open && setConvertingLead(null)}
          title={`Convert Lead to Active ${convertingLead.type === "customer" ? "Customer" : "Supplier"}?`}
          description="This will register a full master record with all contact information and photo attachments."
        >
          <div className="space-y-3 pt-2">
            <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl space-y-1 text-xs">
              <div className="font-bold text-zinc-900 dark:text-zinc-100">{convertingLead.firmName}</div>
              <div className="text-zinc-500">Contact: {convertingLead.name || "Owner"} • {convertingLead.phone}</div>
              <div className="text-zinc-500">Met at: {convertingLead.meetingPlace || convertingLead.city || "Ahmedabad"}</div>
            </div>

            <p className="text-xs text-zinc-600 dark:text-zinc-300">
              Are you sure you want to promote this lead to official {convertingLead.type === "customer" ? "Customer" : "Supplier"} master?
              The lead status will be updated to "Converted".
            </p>

            <div className="flex justify-end gap-2 pt-3 border-t border-zinc-200 dark:border-zinc-800">
              <Button variant="outline" onClick={() => setConvertingLead(null)} className="h-8 text-xs">
                Cancel
              </Button>
              <Button
                onClick={() => handleConvert(convertingLead)}
                className="h-8 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-medium"
              >
                <Check className="h-3.5 w-3.5 mr-1" />
                Yes, Convert Now
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Image Lightbox Modal */}
      <ImageLightboxModal
        open={lightbox.open}
        onClose={() => setLightbox((prev) => ({ ...prev, open: false }))}
        imageUrl={lightbox.url}
        title={lightbox.title}
      />
    </div>
  )
}
