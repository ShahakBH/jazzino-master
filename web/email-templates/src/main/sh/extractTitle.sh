#!/usr/bin/ruby

require 'rexml/document'
include REXML

ARGV.each do|fileRef|
  xmlfile = File.new(fileRef)
  xmldoc = Document.new(xmlfile)
  
  XPath.each(xmldoc, "//title") { |e| puts fileRef + " - " + e.text }
end

