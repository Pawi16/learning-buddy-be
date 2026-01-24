import pdfplumber
import re
from collections import defaultdict

def is_junk(text: str, top: float, page_height: float) -> bool:
    """
    Filters out headers, footers, and page numbers based on position and content.
    """
    # 1. Geometry: Ignore top 10% and bottom 10% of the page (usually headers/footers)
    if top < (page_height * 0.10) or top > (page_height * 0.90):
        return True
    
    # 2. Content: Ignore standalone numbers (Page numbers)
    if re.match(r'^\d+$', text.strip()):
        return True
    
    # 3. Content: Ignore common junk words
    if text.strip().lower().startswith(('figure', 'table', 'box', 'continued')):
        return True
        
    return False

def scan_pdf_stats(pdf_path: str):
    """
    Scans the PDF to build a frequency map of font sizes.
    Returns: (font_stats_dict, list_of_all_text_lines)
    """
    font_stats = defaultdict(lambda: {'count': 0, 'total_len': 0, 'examples': []})
    all_lines = []

    with pdfplumber.open(pdf_path) as pdf:
        # Limit scanning to first 20 pages for speed, or remove slice for full scan
        for page in pdf.pages: 
            words = page.extract_words(extra_attrs=["size", "top"])
            if not words: continue
            
            # Group words into lines
            current_line = [words[0]]
            for word in words[1:]:
                # If word is on the same vertical level (within 5px tolerance)
                if abs(word['top'] - current_line[-1]['top']) < 5:
                    current_line.append(word)
                else:
                    # New line detected
                    _record_line(current_line, font_stats, all_lines, page.height)
                    current_line = [word]
            
            # Record the last line of the page
            _record_line(current_line, font_stats, all_lines, page.height)
            
    return font_stats, all_lines

def _record_line(words, stats, all_lines, h):
    """
    Helper to record a single line's stats.
    """
    if not words: return
    
    # Determine the "dominant" size of the line (usually the max size found)
    size = round(max([w['size'] for w in words]), 1)
    text = " ".join([w['text'] for w in words])
    
    # Update Stats
    stats[size]['count'] += 1
    stats[size]['total_len'] += len(text)
    
    # Keep 5 samples of this font size for the AI to analyze later
    if len(stats[size]['examples']) < 5:
        stats[size]['examples'].append(text[:100])
        
    # Store raw line for the splitting phase
    all_lines.append({
        'text': text, 
        'size': size, 
        'top': words[0]['top'], 
        'height': h
    })

def split_content(all_lines: list, target_sizes: list[float], body_size: float) -> list[dict]:
    """
    Splits the raw text lines into topics based on the Header Sizes provided by AI.
    """
    # Safety: Ensure we don't accidentally split on body text
    valid_targets = {t for t in target_sizes if t > body_size}
    
    topics = []
    current_topic = {"title": "Introduction", "content": ""}
    current_header_size = 0
    
    for line in all_lines:
        text = re.sub(r'\(cid:\d+\)', '', line['text']).strip() # Clean PDF artifacts
        size = line['size']
        
        # Skip Headers/Footers
        if is_junk(text, line['top'], line['height']): 
            continue

        # Check if this line matches one of our "Header Font Sizes"
        is_header = any(abs(size - t) < 0.2 for t in valid_targets)
        
        if is_header:
            # Logic: Is this a continuation of the CURRENT header? (Multi-line titles)
            # If size matches current header AND content is short, append to title.
            if abs(size - current_header_size) < 0.2 and len(current_topic['content']) < 10:
                current_topic['title'] += " " + text
            else:
                # Save previous topic if it has content
                if len(current_topic['content']) > 50: 
                    topics.append(current_topic)
                
                # Start new topic
                current_topic = {"title": text, "content": ""}
                current_header_size = size
        else:
            # It's just body text, append to current topic
            current_topic['content'] += text + "\n"
            
    # Don't forget the last topic
    if current_topic['content']: 
        topics.append(current_topic)
        
    return topics