-- Filters run before --section-divs, so headers are still flat here.
function Pandoc(doc)
  local blocks = doc.blocks

  if blocks[1] and blocks[1].t == 'Header' and blocks[1].level == 1 then
    doc.meta.title = pandoc.MetaInlines(blocks[1].content)
    blocks:remove(1)
  end

  if blocks[1] and blocks[1].t == 'Para' then
    local date = pandoc.utils.stringify(blocks[1]):match('^Effective date:%s*(.+)$')
    if date then
      doc.meta.effective = date
      blocks:remove(1)
    end
  end

  local intro = pandoc.List()
  while blocks[1] and not (blocks[1].t == 'Header' and blocks[1].level == 2) do
    intro:insert(blocks:remove(1))
  end
  if #intro > 0 then
    blocks:insert(1, pandoc.Div(intro, { class = 'intro' }))
  end

  return doc
end
