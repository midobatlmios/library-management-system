import { useEffect, useState } from 'react'
import { IconButton, Tooltip } from '@mui/material'
import FavoriteIcon from '@mui/icons-material/Favorite'
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder'
import { useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'
import { addToWishlist, checkWishlist, removeFromWishlist } from '../../api/wishlist.js'
import { selectIsLoggedIn } from '../../store/authSlice.js'
import { useToast } from '../feedback/toastContext.js'

/** Heart toggle. Visitors are sent to log in first. */
export default function WishlistButton({ bookId }) {
  const onDone = useToast()
  const loggedIn = useSelector(selectIsLoggedIn)
  const navigate = useNavigate()
  const location = useLocation()
  const [inWishlist, setInWishlist] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let cancelled = false
    checkWishlist(bookId)
      .then((r) => !cancelled && setInWishlist(Boolean(r.isInWishlist)))
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [bookId, loggedIn])

  const toggle = async () => {
    if (!loggedIn) {
      navigate('/login', { state: { from: location } })
      return
    }
    setBusy(true)
    try {
      if (inWishlist) {
        await removeFromWishlist(bookId)
        setInWishlist(false)
        onDone('Removed from your wishlist', 'info')
      } else {
        await addToWishlist(bookId)
        setInWishlist(true)
        onDone('Added to your wishlist', 'success')
      }
    } catch (e) {
      onDone(e.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  const label = inWishlist ? 'Remove from wishlist' : 'Add to wishlist'
  return (
    <Tooltip title={label}>
      <span>
        <IconButton onClick={toggle} disabled={busy} aria-label={label} color={inWishlist ? 'error' : 'default'}>
          {inWishlist ? <FavoriteIcon /> : <FavoriteBorderIcon />}
        </IconButton>
      </span>
    </Tooltip>
  )
}
